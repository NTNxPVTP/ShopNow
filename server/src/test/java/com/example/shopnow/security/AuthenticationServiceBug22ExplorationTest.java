package com.example.shopnow.security;

import com.example.shopnow.security.rest.dto.AuthenticationRequest;
import com.example.shopnow.user.api.UserApi;
import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Exploration test for BUG-22 (authenticate không @Transactional).
 *
 * <p><b>Validates: Requirements 2.22</b> (Property 22 in design.md).
 *
 * <p>Phase 1 bug-condition exploration — MUST FAIL on unfixed code = SUCCESS.
 *
 * <p><b>Bug condition C(22):</b> {@code AuthenticationService.authenticate}
 * calls {@code revokeAllUserTokens} then {@code saveUserTokens} without a
 * {@code @Transactional} boundary. If {@code saveUserTokens} fails (e.g.
 * {@code DataIntegrityViolationException} from {@code tokenRepository.saveAll}),
 * the revoke has already committed — user's old tokens are gone but no new
 * tokens exist, leaving the user locked out.
 *
 * <p><b>Property P(22):</b> {@code authenticate} method SHALL be annotated
 * {@code @Transactional} (Spring or Jakarta). If {@code saveUserTokens} fails
 * after {@code revokeAllUserTokens}, the transaction SHALL rollback so old
 * tokens remain valid.
 *
 * <p><b>Test approach:</b>
 * <ol>
 *   <li>Reflection check: verify {@code @Transactional} annotation is present
 *       on the {@code authenticate} method.</li>
 *   <li>Behavioral demonstration: mock a scenario where authenticate succeeds
 *       through revokeAllUserTokens, then saveUserTokens throws
 *       {@code DataIntegrityViolationException}. Without @Transactional, the
 *       revoke side-effect is NOT rolled back (we verify the annotation is
 *       the mechanism that would provide rollback).</li>
 * </ol>
 *
 * <p><b>Expected on unfixed code:</b> FAIL — annotation is null, confirming
 * the bug exists (no transactional boundary to protect consistency).
 */
class AuthenticationServiceBug22ExplorationTest {

    private static final String EMAIL = "bob@example.com";
    private static final String PASSWORD = "validPassword123";

    private UserApi userService;
    private org.springframework.security.authentication.AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private TokenService tokenService;

    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserApi.class);
        authenticationManager = Mockito.mock(org.springframework.security.authentication.AuthenticationManager.class);
        jwtService = Mockito.mock(JwtService.class);
        tokenService = Mockito.mock(TokenService.class);

        service = new AuthenticationService(
                userService, authenticationManager, jwtService, tokenService);
    }

    /**
     * Core assertion for BUG-22: the {@code authenticate} method MUST have
     * {@code @Transactional} annotation so that revokeAllUserTokens and
     * saveUserTokens share a single transaction boundary.
     *
     * <p>On unfixed code this FAILS because the method has no @Transactional.
     */
    @Test
    @DisplayName("BUG-22: authenticate SHALL be annotated @Transactional for rollback on saveUserTokens failure")
    void authenticate_shallBeAnnotatedTransactional_forRollbackGuarantee() throws NoSuchMethodException {
        Method authMethod = AuthenticationService.class.getMethod(
                "authenticate", AuthenticationRequest.class);

        Annotation springTx = authMethod.getAnnotation(
                org.springframework.transaction.annotation.Transactional.class);
        Annotation jakartaTx = authMethod.getAnnotation(
                jakarta.transaction.Transactional.class);

        assertThat(springTx != null || jakartaTx != null)
                .as("BUG-22 (Requirements 2.22): AuthenticationService.authenticate SHALL be "
                        + "annotated @Transactional (Spring or Jakarta). Without it, "
                        + "revokeAllUserTokens commits independently — if saveUserTokens "
                        + "subsequently throws DataIntegrityViolationException, old tokens "
                        + "are already revoked with no new tokens created, locking the user out. "
                        + "Found springTx=%s, jakartaTx=%s", springTx, jakartaTx)
                .isTrue();
    }

    /**
     * Behavioral demonstration: when saveUserTokens throws after
     * revokeAllUserTokens has been called, the lack of @Transactional means
     * the revoke side-effect cannot be rolled back.
     *
     * <p>This test verifies that:
     * <ol>
     *   <li>revokeAllUserTokens IS called before saveUserTokens (ordering)</li>
     *   <li>When saveUserTokens throws, the exception propagates (no catch)</li>
     *   <li>Without @Transactional, there is no mechanism to undo the revoke</li>
     * </ol>
     *
     * <p>The annotation check above is the definitive assertion for BUG-22.
     * This test supplements it by demonstrating the failure scenario.
     */
    @Test
    @DisplayName("BUG-22: saveUserTokens failure after revokeAllUserTokens — no rollback without @Transactional")
    void authenticate_saveUserTokensFailure_revokeNotRolledBack() {
        // Arrange: authenticate succeeds, user found, tokens generated
        User user = User.builder().email(EMAIL).password(PASSWORD).build();

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userService.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("new-refresh-token");

        // revokeAllUserTokens succeeds (side-effect: old tokens marked revoked)
        doNothing().when(tokenService).revokeAllUserTokens(user);

        // saveUserTokens throws DataIntegrityViolationException (simulating DB error)
        doThrow(new DataIntegrityViolationException("Simulated saveAll failure"))
                .when(tokenService).saveUserTokens(user, "new-access-token", "new-refresh-token");

        // Act
        Throwable thrown = catchThrowable(() ->
                service.authenticate(new AuthenticationRequest(EMAIL, PASSWORD)));

        // Assert: exception propagates (no internal catch for this scenario)
        assertThat(thrown)
                .as("DataIntegrityViolationException from saveUserTokens should propagate")
                .isInstanceOf(DataIntegrityViolationException.class);

        // Verify ordering: revokeAllUserTokens was called BEFORE saveUserTokens
        InOrder inOrder = inOrder(tokenService);
        inOrder.verify(tokenService).revokeAllUserTokens(user);
        inOrder.verify(tokenService).saveUserTokens(user, "new-access-token", "new-refresh-token");

        // The critical point: revokeAllUserTokens was called and completed
        // successfully. Without @Transactional, this side-effect is permanent
        // even though saveUserTokens failed afterwards. The annotation check
        // in the test above is what confirms the bug — this test just
        // demonstrates the problematic execution flow.
        verify(tokenService, times(1)).revokeAllUserTokens(user);
    }
}
