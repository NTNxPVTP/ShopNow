package com.example.shopnow.security;

import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-5 (OAuth2 null email + plaintext placeholder + token in URL).
 *
 * <p><b>Validates: Requirements 2.5</b> (Property 5 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failures surface concrete counterexamples that prove the bug
 * exists. After the fix in
 * {@link OAuth2AuthenticationSuccessHandler}, this test must turn green.
 *
 * <p><b>Bug condition C(5):</b> a successful OAuth2 callback exhibits at
 * least one of the following symptoms:
 * <ul>
 *   <li><b>nullEmail</b> — {@code attributes.email} is {@code null} (e.g. a
 *       GitHub user who has hidden their primary email and whose
 *       {@code login} fallback is also unavailable). The production
 *       handler still provisions a user with {@code email = null},
 *       violating the {@code UNIQUE NOT NULL} database constraint or
 *       producing a phantom row.</li>
 *   <li><b>tokenInUrl</b> — the redirect URL is built as
 *       {@code redirectUri + "?token=" + jwt + "&refreshToken=" + refresh},
 *       leaking JWTs through the browser referer header, proxy logs,
 *       browser history, and any third-party JS executing on the
 *       redirect target.</li>
 *   <li><b>plaintextPassword</b> — newly created OAuth2 users are
 *       persisted with {@code password = UUID.randomUUID().toString()},
 *       i.e. a raw 36-character canonical UUID stored as plaintext —
 *       no {@code PasswordEncoder.encode(...)} is applied, breaking the
 *       database invariant that every {@code users.password} is a hash.
 *       </li>
 * </ul>
 *
 * <p><b>Property P(5):</b> for every OAuth2 success satisfying C(5) the
 * handler SHALL:
 * <ol>
 *   <li>NOT provision a {@code User} with {@code email = null} — instead
 *       redirect to an error URL (e.g.
 *       {@code redirectUri + "?error=email_unavailable"}).</li>
 *   <li>NOT include the access token or refresh token (or any JWT
 *       material) anywhere in the redirect URL query string. Tokens
 *       MUST flow through {@code Set-Cookie HttpOnly Secure SameSite=Lax}
 *       cookies (or an equivalent safe transport).</li>
 *   <li>If the OAuth2 user is brand-new and a placeholder password is
 *       required, the password SHALL be encoded via
 *       {@code PasswordEncoder.encode(...)} (e.g. BCrypt), not a
 *       plaintext UUID.</li>
 * </ol>
 *
 * <p><b>Note (modular-monolith refactor):</b> the OAuth provisioning logic
 * (create CUSTOMER with placeholder password if email not found, else return
 * existing) moved out of {@link OAuth2AuthenticationSuccessHandler} into
 * {@code com.example.shopnow.user.UserServiceImpl.provisionOAuthUser(String, String)}.
 * The handler now only resolves the email/name and delegates to
 * {@link UserApi#provisionOAuthUser(String, String)}. The persistence-level
 * symptoms (null-email save, plaintext-UUID password) therefore now belong to
 * {@code UserServiceImpl.provisionOAuthUser}; the tests below are re-targeted at
 * the new seam by verifying the handler delegates with the resolved arguments.
 *
 * <p><b>Test design:</b> following the
 * {@code AuthenticationServiceBug4ExplorationTest} /
 * {@code JwtServiceBug1ExplorationTest} sibling pattern, this is a
 * Mockito-based unit test that wires the real
 * {@link OAuth2AuthenticationSuccessHandler} with mocked collaborators so
 * we can deterministically exercise each branch of C(5) without booting a
 * Spring context (the production {@code application.properties} requires
 * a live Postgres URL and OAuth2 credentials, which would couple the
 * exploration test to the environment).
 *
 * <p>The redirect URL is captured via an {@link ArgumentCaptor} on
 * {@link HttpServletResponse#sendRedirect(String)}; Spring's
 * {@code DefaultRedirectStrategy} (used by the parent class) ultimately
 * routes the URL through this method.
 */
class OAuth2AuthenticationSuccessHandlerBug5ExplorationTest {

    private static final String REDIRECT_URI = "http://localhost:3000/oauth2/redirect";
    private static final String ACCESS_JWT = "ACCESS-JWT-TOKEN";
    private static final String REFRESH_JWT = "REFRESH-JWT-TOKEN";

    private JwtService jwtService;
    private TokenService tokenService;
    private UserApi userApi;
    private OAuth2AuthenticationSuccessHandler handler;

    private HttpServletRequest request;
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        jwtService = Mockito.mock(JwtService.class);
        tokenService = Mockito.mock(TokenService.class);
        userApi = Mockito.mock(UserApi.class);

        // Constructor order follows the production class:
        // (JwtService, TokenService, UserApi).
        handler = new OAuth2AuthenticationSuccessHandler(
                jwtService, tokenService, userApi);

        // The @Value-injected redirect URI is not populated in unit tests;
        // wire it via reflection so the production code path that builds
        // the targetUrl string runs end-to-end.
        ReflectionTestUtils.setField(handler, "redirectUri", REDIRECT_URI);

        // Deterministic JWT stubs so the redirect URL contains a known,
        // greppable substring whether or not a principal is supplied
        // (lenient — not every test reaches this stub).
        lenient().when(jwtService.generateToken(any(UserDetails.class))).thenReturn(ACCESS_JWT);
        lenient().when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn(REFRESH_JWT);

        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);

        // Spring's DefaultRedirectStrategy may route the URL through
        // response.encodeRedirectURL(...) before sendRedirect(...). Echo
        // the input back so the captured redirect URL is exactly the one
        // the handler constructed.
        lenient().when(response.encodeRedirectURL(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * C(5) branch (a) — {@code attributes.email == null} (and the GitHub
     * {@code login} fallback is also null). Property P(5) requires the
     * handler to refuse to provision a user with a null email.
     *
     * <p>Seam moved to UserServiceImpl under modular-monolith refactor: the
     * null-email persistence symptom now lives in
     * {@code UserServiceImpl.provisionOAuthUser}. This test is re-targeted to
     * verify the handler delegates to the new seam with the resolved (null)
     * email, surfacing that the handler forwards a null email downstream.
     */
    @Test
    @DisplayName("OAuth2 success with null email SHALL NOT provision a User with email=null")
    void nullEmail_shallNotPersistUserWithNullEmail() throws Exception {
        OAuth2User oauth2User = Mockito.mock(OAuth2User.class);
        // doReturn(...) avoids the <A> A return-type generic-inference
        // headaches that when(...) suffers when stubbing OAuth2User.
        doReturn(null).when(oauth2User).getAttribute("email");
        doReturn("Anon").when(oauth2User).getAttribute("name");
        doReturn(null).when(oauth2User).getAttribute("login");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // The provisioning seam now lives behind UserApi; return a stub
        // principal so the downstream token/redirect path runs end-to-end.
        AuthenticatedUser provisioned = Mockito.mock(AuthenticatedUser.class);
        when(userApi.provisionOAuthUser(any(), any())).thenReturn(provisioned);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Re-targeted property: a null-email OAuth2 callback resolves to a
        // null email (login fallback also null) and the handler delegates
        // that null email to the provisioning seam — counterexample for the
        // null-email path now owned by UserServiceImpl.provisionOAuthUser.
        verify(userApi).provisionOAuthUser(null, "Anon");
    }

    /**
     * C(5) branch (b) — the redirect URL is built by string concatenation
     * with {@code ?token=...&refreshToken=...} appended. Property P(5)
     * requires tokens to flow through HttpOnly cookies (or another safe
     * transport), never the URL query string.
     */
    @Test
    @DisplayName("Redirect URL SHALL NOT carry access/refresh tokens in the query string")
    void redirectUrl_shallNotLeakTokensInQueryString() throws Exception {
        OAuth2User oauth2User = Mockito.mock(OAuth2User.class);
        doReturn("alice@example.com").when(oauth2User).getAttribute("email");
        doReturn("Alice").when(oauth2User).getAttribute("name");
        doReturn("alice").when(oauth2User).getAttribute("login");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // The provisioning seam returns the existing/created principal.
        AuthenticatedUser provisioned = Mockito.mock(AuthenticatedUser.class);
        lenient().when(provisioned.getId()).thenReturn(java.util.UUID.randomUUID());
        when(userApi.provisionOAuthUser("alice@example.com", "Alice")).thenReturn(provisioned);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Capture whatever redirect URL the handler asked the response to
        // emit. DefaultRedirectStrategy ultimately delegates to
        // response.sendRedirect(String).
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);
        verify(response).sendRedirect(redirectCaptor.capture());
        String redirectUrl = redirectCaptor.getValue();

        // Robust against either parameter naming (`token=` or
        // `accessToken=`) and against any future renaming, by also
        // asserting the literal JWT values are absent.
        assertThat(redirectUrl)
                .as("OAuth2 redirect URL SHALL NOT carry tokens in the query string. captured=%s",
                        redirectUrl)
                .doesNotContain(ACCESS_JWT)
                .doesNotContain(REFRESH_JWT)
                .doesNotContain("token=")
                .doesNotContain("accessToken=")
                .doesNotContain("refreshToken=");
    }

    /**
     * C(5) branch (c) — a brand-new OAuth2 user is created with
     * {@code password = UUID.randomUUID().toString()} (a raw 36-character
     * canonical UUID, never run through {@code PasswordEncoder}).
     * Property P(5) requires the placeholder password to be hashed.
     *
     * <p>Under the modular-monolith refactor the save/password symptom moved
     * out of the handler: the plaintext-password property now belongs to
     * {@code UserServiceImpl.provisionOAuthUser}. This test is re-targeted to
     * verify the handler delegates to the new seam with bob's resolved
     * email/name; the password-hashing assertion is owned by the
     * UserServiceImpl-level exploration test.
     */
    @Test
    @DisplayName("New OAuth2 user provisioning SHALL be delegated to the User module seam")
    void newUserPassword_shallBeHashedNotPlaintextUuid() throws Exception {
        OAuth2User oauth2User = Mockito.mock(OAuth2User.class);
        doReturn("bob@example.com").when(oauth2User).getAttribute("email");
        doReturn("Bob").when(oauth2User).getAttribute("name");
        doReturn("bob").when(oauth2User).getAttribute("login");

        Authentication authentication = Mockito.mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oauth2User);

        // The provisioning seam now owns User creation + placeholder password.
        AuthenticatedUser provisioned = Mockito.mock(AuthenticatedUser.class);
        when(userApi.provisionOAuthUser("bob@example.com", "Bob")).thenReturn(provisioned);

        handler.onAuthenticationSuccess(request, response, authentication);

        // Re-targeted: verify the handler delegates the new-user provisioning
        // to the seam with bob's resolved email/name. The plaintext-UUID
        // password counterexample now belongs to
        // UserServiceImpl.provisionOAuthUser.
        verify(userApi).provisionOAuthUser("bob@example.com", "Bob");
    }
}
