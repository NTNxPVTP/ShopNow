package com.example.shopnow.security;

import com.example.shopnow.security.models.Token;
import com.example.shopnow.security.models.TokenType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exploration test for BUG-2 (JwtException → 500 Internal Server Error).
 *
 * <p><b>Validates: Requirements 2.2</b> (Property 2 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * `shopnow-codebase-bugfixes`. It is EXPECTED TO FAIL on unfixed code; the
 * failure surfaces a counterexample that proves the bug exists. After the
 * fix in {@link JwtFilter}, this test must turn green.
 *
 * <p><b>Bug condition C(2):</b> a request carries
 * {@code Authorization: Bearer X} where {@code X} is one of:
 * <ul>
 *   <li>malformed (e.g. {@code abc.def.ghi}),</li>
 *   <li>signed with a key different from the configured secret,</li>
 *   <li>structurally valid but past its {@code exp} claim.</li>
 * </ul>
 *
 * <p><b>Property P(2):</b> the response SHALL have
 * {@code status == 401 Unauthorized} AND
 * {@code Content-Type == application/problem+json} (RFC 7807 ProblemDetail).
 *
 * <p><b>Buggy production behaviour (in {@link JwtFilter#doFilterInternal}):</b>
 * <pre>
 *   userName = jwtService.extractUsername(jwt);   // throws JwtException
 *   // no try/catch -> exception bubbles out of filter chain
 *   //              -> servlet container responds HTTP 500
 *   //              -> Content-Type defaults to text/plain or text/html
 * </pre>
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code Authorization: Bearer abc.def.ghi} → {@code MalformedJwtException}
 * propagates out of the filter, response is {@code 500 Internal Server Error}
 * (or the test thread receives the wrapped servlet exception) instead of the
 * required {@code 401 application/problem+json}.
 *
 * <p>Test wiring uses standalone MockMvc with the real {@link JwtFilter} and
 * real {@link JwtService} (with mocked {@link TokenRepository} and
 * {@link UserDetailsService}) to keep the exploration focused on the filter's
 * exception-handling boundary, without needing a full Spring Boot context.
 */
class JwtFilterBug2ExplorationTest {

    /** 296-bit HS256 secret (Base64-encoded), shared with the configured JwtService. */
    private static final String BASE64_SECRET =
            "c2hvcG5vd190ZXN0X3NlY3JldF9rZXlfMzJfYnl0ZXNfbG9uZw==";

    /** A different 256+ bit key, used to forge a "valid-looking" JWT with bad signature. */
    private static final String BASE64_OTHER_SECRET =
            "YW5vdGhlcl9zZWNyZXRfa2V5X2Zvcl9iYWRfc2lnbmF0dXJlX3Rlc3Q=";

    private static final long ACCESS_TTL_MS = 86_400_000L;
    private static final long REFRESH_TTL_MS = 604_800_000L;
    private static final String USERNAME = "alice@example.com";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TokenRepository tokenRepository = Mockito.mock(TokenRepository.class);
        UserDetailsService userDetailsService = Mockito.mock(UserDetailsService.class);

        // The bug surfaces inside extractUsername (parseClaimsJws) before the
        // filter touches the repository or the user-details service. Stub them
        // permissively so that, on POST-FIX code, the filter would still be
        // exercised end-to-end without NPE.
        lenient().when(tokenRepository.findByToken(anyString()))
                .thenReturn(Optional.of(Token.builder()
                        .token("anything")
                        .tokenType(TokenType.ACCESS_TOKEN)
                        .expired(false)
                        .revoked(false)
                        .build()));
        lenient().when(userDetailsService.loadUserByUsername(anyString()))
                .thenReturn(new User(USERNAME, "irrelevant", java.util.Collections.emptyList()));

        JwtService jwtService = new JwtService(tokenRepository);
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_TTL_MS);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", REFRESH_TTL_MS);

        JwtFilter jwtFilter = new JwtFilter(jwtService, userDetailsService, tokenRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProbeController())
                .addFilters(jwtFilter)
                .build();
    }

    /**
     * Three counterexamples covering the bug condition's three branches:
     * malformed token, bad-signature token, and expired token. All MUST be
     * rejected by JwtFilter with HTTP 401 + application/problem+json.
     */
    static Stream<Arguments> invalidJwts() {
        long now = System.currentTimeMillis();
        return Stream.of(
                Arguments.of("malformed",
                        "abc.def.ghi"),
                Arguments.of("badSignature",
                        buildJwt(BASE64_OTHER_SECRET, USERNAME,
                                new Date(now), new Date(now + ACCESS_TTL_MS))),
                Arguments.of("expired",
                        buildJwt(BASE64_SECRET, USERNAME,
                                new Date(now - 2 * ACCESS_TTL_MS),
                                new Date(now - ACCESS_TTL_MS))));
    }

    @ParameterizedTest(name = "[{index}] {0} JWT -> 401 application/problem+json")
    @MethodSource("invalidJwts")
    @DisplayName("JwtFilter SHALL translate every JwtException into 401 ProblemDetail")
    void invalidJwt_shall_yield_401_problemJson(String label, String invalidJwt) throws Exception {
        // --- Act + Assert ---------------------------------------------------
        // On unfixed code: extractUsername throws JwtException; the filter
        // does not catch it; the servlet container produces 500 (or MockMvc
        // re-throws a wrapped servlet exception). Either way, this assertion
        // chain fails and surfaces the counterexample.
        //
        // On fixed code: the filter catches JwtException, sets status 401
        // and writes a ProblemDetail body with Content-Type
        // application/problem+json.
        mockMvc.perform(get("/probe")
                        .header("Authorization", "Bearer " + invalidJwt))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build a JWT signed with the supplied Base64-encoded HMAC secret. Allows
     * forging both bad-signature tokens (different secret) and expired tokens
     * (issuedAt / expiration in the past). Issuer/audience are omitted to
     * stay focused on the parsing failure modes targeted by BUG-2.
     */
    private static String buildJwt(String base64Secret, String subject, Date issuedAt, Date expiresAt) {
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(subject)
                .setIssuedAt(issuedAt)
                .setExpiration(expiresAt)
                .signWith(signKey(base64Secret), SignatureAlgorithm.HS256)
                .compact();
    }

    private static Key signKey(String base64Secret) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /** Minimal protected probe endpoint to exercise the filter chain. */
    @RestController
    static class ProbeController {
        @GetMapping("/probe")
        String probe() {
            return "ok";
        }

        // Suppress unused-warning markers; keep the controller intentionally
        // tiny so the filter is the only meaningful component under test.
        @SuppressWarnings("unused")
        private UUID neverUsed;
    }
}
