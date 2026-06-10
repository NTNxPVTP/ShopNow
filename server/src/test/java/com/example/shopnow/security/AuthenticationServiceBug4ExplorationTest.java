package com.example.shopnow.security;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.security.rest.dto.AuthenticationRequest;
import com.example.shopnow.user.api.UserApi;
import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-4 (authenticate bubbles raw exceptions + non-transactional).
 *
 * <p><b>Validates: Requirements 2.4, 2.22</b> (Property 4 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed code;
 * the failure surfaces counterexamples that prove the bug exists. After the
 * fix in {@link AuthenticationService}, this test must turn green.
 *
 * <p><b>Bug condition C(4):</b> a {@code POST /authenticate} request hits one
 * of the two fail-path branches:
 * <ul>
 *   <li><b>wrongPassword</b> — email exists in the DB but the password is
 *       wrong, so {@link AuthenticationManager#authenticate(org.springframework.security.core.Authentication)}
 *       raises {@link BadCredentialsException};</li>
 *   <li><b>emailNotFound</b> — no row with that email, so the chained
 *       {@code userService.findByEmail(...).orElseThrow()} raises
 *       {@link java.util.NoSuchElementException}.</li>
 * </ul>
 * Additionally, the surrounding {@code authenticate(...)} method has no
 * transactional boundary, so a failure of {@code saveUserTokens} after a
 * successful {@code revokeAllUserTokens} cannot be rolled back as a unit.
 *
 * <p><b>Property P(4):</b> for every input satisfying C(4), the service
 * SHALL throw a {@link DomainException} whose {@code errorCode.name()}
 * equals {@code "INVALID_CREDENTIALS"} (mapped to HTTP 401 by
 * {@code GlobalExceptionHandler}). The framework SHALL NOT see raw
 * {@link BadCredentialsException} / {@link java.util.NoSuchElementException}
 * (which would bubble out as 500 / {@code UNCATEGORIZED_EXCEPTION}). The
 * {@code authenticate} method SHALL be annotated {@code @Transactional}
 * (Spring or Jakarta), so {@code revokeAllUserTokens} and
 * {@code saveUserTokens} share a single transaction and a downstream
 * {@code DataIntegrityViolationException} from {@code saveAll} rolls back
 * the prior revoke.
 *
 * <p><b>Buggy production behaviour:</b>
 * <pre>
 *   public AuthenticationResponse authenticate(AuthenticationRequest request) {
 *       authenticationManager.authenticate(            // (a) raw BadCredentialsException leaks
 *               new UsernamePasswordAuthenticationToken(request.email(), request.password()));
 *       var user = userService.findByEmail(request.email()).orElseThrow();   // (b) raw NoSuchElementException leaks
 *       ...
 *       tokenService.revokeAllUserTokens(user);    // commits independently — not rolled back when next line fails
 *       tokenService.saveUserTokens(user, jwtToken, refreshToken);
 *       ...
 *   }
 *   // No @Transactional — see Requirements 2.22.
 * </pre>
 *
 * <p><b>Test design:</b> following the {@code AuthenticationServiceBug1ExplorationTest}
 * / {@code JwtFilterBug2ExplorationTest} sibling pattern, this is a
 * Mockito-based unit test that wires the real {@link AuthenticationService}
 * with mocked collaborators so we can exercise both fail-path branches
 * deterministically without booting a Spring context (the production
 * {@code application.properties} requires a live Postgres URL and OAuth2
 * credentials, which would couple the exploration test to the environment).
 *
 * <p>The transactional contract from Requirements 2.22 is asserted via
 * reflection on the {@code authenticate(AuthenticationRequest)} method
 * — the only deterministic, environment-independent way to verify that the
 * caller-side rollback boundary exists.
 *
 * <p><b>Expected counterexamples on unfixed code:</b>
 * <ul>
 *   <li>{@code WRONG_PASSWORD} → service throws {@code BadCredentialsException}
 *       instead of {@code DomainException(INVALID_CREDENTIALS)} → this test
 *       fails the {@code isInstanceOf(DomainException.class)} assertion.</li>
 *   <li>{@code EMAIL_NOT_FOUND} → service throws {@code NoSuchElementException}
 *       (from {@code Optional.orElseThrow()}) instead of
 *       {@code DomainException(INVALID_CREDENTIALS)} → same assertion fails.</li>
 *   <li>{@code authenticate} method has no {@code @Transactional} annotation
 *       (neither Spring nor Jakarta) → reflection assertion fails, surfacing
 *       the missing transactional boundary that would have rolled back
 *       {@code revokeAllUserTokens} on a downstream {@code saveAll} failure.</li>
 * </ul>
 */
class AuthenticationServiceBug4ExplorationTest {

    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD = "doesntmatter";

    private UserApi userService;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private TokenService tokenService;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserApi.class);
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        jwtService = Mockito.mock(JwtService.class);
        tokenService = Mockito.mock(TokenService.class);

        // Order of constructor params follows the production class:
        // (UserService, AuthenticationManager, JwtService, TokenService).
        service = new AuthenticationService(
                userService, authenticationManager, jwtService, tokenService);

        // Permissive lenient stubs for the happy-path tail. Each scenario
        // overrides the relevant collaborator below; lenient() avoids
        // "unnecessary stubbing" warnings when a branch never reaches them.
        lenient().when(jwtService.generateToken(any(User.class))).thenReturn("access-jwt");
        lenient().when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-jwt");
    }

    /**
     * The two fail-path branches called out by the bug condition. Encoding
     * the scenarios as an enum keeps the parameterized test self-documenting
     * — the JUnit display name carries {@code WRONG_PASSWORD} or
     * {@code EMAIL_NOT_FOUND} verbatim into the failure message.
     */
    enum FailScenario {
        /** Email exists in the DB but the password supplied is wrong. */
        WRONG_PASSWORD,
        /** No user row with that email; {@code findByEmail(..).orElseThrow()} blows up. */
        EMAIL_NOT_FOUND
    }

    @ParameterizedTest(name = "[{index}] {0} -> DomainException(INVALID_CREDENTIALS)")
    @EnumSource(FailScenario.class)
    @DisplayName("authenticate fail-path SHALL throw DomainException(INVALID_CREDENTIALS), not bubble raw exceptions")
    void authenticate_failPath_shallThrowDomainExceptionInvalidCredentials(FailScenario scenario) {
        // --- Arrange --------------------------------------------------------
        AuthenticationRequest req = new AuthenticationRequest(EMAIL, PASSWORD);

        switch (scenario) {
            case WRONG_PASSWORD -> {
                // authenticationManager rejects the credentials with the
                // standard Spring Security exception. On unfixed code this
                // exception bubbles straight out of authenticate(...).
                when(authenticationManager.authenticate(
                        any(UsernamePasswordAuthenticationToken.class)))
                        .thenThrow(new BadCredentialsException("Bad credentials"));
            }
            case EMAIL_NOT_FOUND -> {
                // authenticationManager passes (return value is discarded by
                // the production code), but the lookup that follows finds no
                // user row, so .orElseThrow() raises NoSuchElementException
                // on unfixed code.
                when(authenticationManager.authenticate(
                        any(UsernamePasswordAuthenticationToken.class)))
                        .thenReturn(null);
                when(userService.findByEmail(anyString())).thenReturn(Optional.empty());
            }
        }

        // --- Act ------------------------------------------------------------
        Throwable thrown = catchThrowable(() -> service.authenticate(req));

        // --- Assert ---------------------------------------------------------
        // P(4) requires DomainException to surface here, NOT the raw upstream
        // exception. On unfixed code this assertion fails with the precise
        // counterexample (BadCredentialsException for WRONG_PASSWORD,
        // NoSuchElementException for EMAIL_NOT_FOUND).
        assertThat(thrown)
                .as("scenario=%s — authenticate SHALL surface DomainException, not bubble raw exception", scenario)
                .isInstanceOf(DomainException.class);

        DomainException de = (DomainException) thrown;

        // ErrorCode.INVALID_CREDENTIALS does not exist in the unfixed enum;
        // matching by name() (rather than the enum constant directly) keeps
        // this exploration test compilable on unfixed code while still
        // pinning the post-fix expectation.
        assertThat(de.getErrorCode())
                .as("scenario=%s — DomainException SHALL carry an ErrorCode", scenario)
                .isNotNull();
        assertThat(de.getErrorCode().name())
                .as("scenario=%s — ErrorCode.name() SHALL be INVALID_CREDENTIALS (HTTP 401)", scenario)
                .isEqualTo("INVALID_CREDENTIALS");
    }

    /**
     * Property P(4) / Requirements 2.22 contract: {@code authenticate(...)}
     * must run inside a single transaction so that a failure of
     * {@code tokenService.saveUserTokens} (e.g. {@code saveAll} throwing
     * {@code DataIntegrityViolationException}) rolls back the in-flight
     * {@code revokeAllUserTokens} side effect. A unit-style test cannot
     * meaningfully observe DB-level rollback (no real EntityManager), so we
     * verify the contract by reflection: the transactional boundary lives on
     * the method annotation itself. If neither
     * {@link org.springframework.transaction.annotation.Transactional} nor
     * {@link jakarta.transaction.Transactional} is present, Spring wires no
     * proxy-driven rollback and the bug remains.
     */
    @Test
    @DisplayName("authenticate SHALL be annotated @Transactional so revoke + save share one tx")
    void authenticate_shallBeAnnotatedTransactional() throws NoSuchMethodException {
        Method authMethod = AuthenticationService.class.getMethod(
                "authenticate", AuthenticationRequest.class);

        Annotation springTx = authMethod.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class);
        Annotation jakartaTx = authMethod.getAnnotation(
                jakarta.transaction.Transactional.class);

        assertThat(springTx != null || jakartaTx != null)
                .as("AuthenticationService.authenticate SHALL be annotated @Transactional "
                        + "(Spring or Jakarta) per Property 4 (Requirements 2.22). "
                        + "Without it, revokeAllUserTokens commits even when saveUserTokens "
                        + "fails afterwards, leaving the user with no valid tokens. "
                        + "Found springTx=%s, jakartaTx=%s",
                        springTx, jakartaTx)
                .isTrue();
    }
}
