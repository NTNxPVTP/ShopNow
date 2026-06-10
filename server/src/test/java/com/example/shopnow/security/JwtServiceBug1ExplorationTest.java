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
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-1 (JWT operator precedence).
 *
 * Validates: Requirements 2.1 (Property 1)
 *
 * <p>This is a Phase 1 bug-condition exploration test for the bugfix workflow
 * `shopnow-codebase-bugfixes`. It is EXPECTED TO FAIL on unfixed code; the
 * failure surfaces a counterexample that proves the bug exists. After fix,
 * this test must turn green.
 *
 * <p>Bug condition C(1): a JWT whose persisted Token row has
 *   {@code tokenType == REFRESH_TOKEN}, {@code expired == false},
 *   {@code revoked == false}, and a valid signature, is presented as an
 *   access token to {@link JwtService#isAccessTokenValid(String, UserDetails)}.
 *
 * <p>Property P(1): for every input satisfying C(1),
 *   {@code isAccessTokenValid} SHALL return {@code false}.
 *
 * <p>Buggy expression in production:
 * <pre>
 *   tokenDB == null || tokenDB.isExpired() || tokenDB.isRevoked() &amp;&amp; tokenDB.getTokenType() == tokenType
 * </pre>
 * Java binds {@code &&} tighter than {@code ||}, so for a refresh token with
 * {@code expired=false, revoked=false} the early-return is skipped and the
 * method incorrectly returns {@code true}.
 *
 * <p>Correct expression after fix:
 * <pre>
 *   tokenDB == null || tokenDB.isExpired() || tokenDB.isRevoked() || tokenDB.getTokenType() != tokenType
 * </pre>
 */
class JwtServiceBug1ExplorationTest {

    /** 256-bit HS256 secret (Base64-encoded). Sufficient for jjwt key length check. */
    private static final String BASE64_SECRET =
            "c2hvcG5vd190ZXN0X3NlY3JldF9rZXlfMzJfYnl0ZXNfbG9uZw==";

    private static final long ACCESS_TTL_MS = 86_400_000L;
    private static final long REFRESH_TTL_MS = 604_800_000L;

    private static final String USERNAME = "alice@example.com";

    private TokenRepository tokenRepository;
    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        tokenRepository = Mockito.mock(TokenRepository.class);
        jwtService = new JwtService(tokenRepository);
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_TTL_MS);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", REFRESH_TTL_MS);

        userDetails = new User(
                USERNAME,
                "irrelevant",
                Collections.emptyList());
    }

    /**
     * Cover the (tokenType x expired x revoked) cube with a valid signature.
     * The flag {@code expectedValid} encodes the correct (post-fix) expected
     * result of {@link JwtService#isAccessTokenValid}: only ACCESS tokens that
     * are neither expired nor revoked SHALL be accepted.
     *
     * <p>The bug-condition row is the one where
     * {@code dbType == REFRESH_TOKEN, dbExpired == false, dbRevoked == false}.
     */
    static Stream<Arguments> tokenStateCube() {
        return Stream.of(
                // ACCESS tokens — valid only when not expired and not revoked
                Arguments.of(TokenType.ACCESS_TOKEN,  false, false, true),
                Arguments.of(TokenType.ACCESS_TOKEN,  true,  false, false),
                Arguments.of(TokenType.ACCESS_TOKEN,  false, true,  false),
                Arguments.of(TokenType.ACCESS_TOKEN,  true,  true,  false),
                // REFRESH tokens — NEVER valid as access tokens regardless of flags
                Arguments.of(TokenType.REFRESH_TOKEN, false, false, false), // <-- BUG-1 counterexample
                Arguments.of(TokenType.REFRESH_TOKEN, true,  false, false),
                Arguments.of(TokenType.REFRESH_TOKEN, false, true,  false),
                Arguments.of(TokenType.REFRESH_TOKEN, true,  true,  false));
    }

    @ParameterizedTest(name = "[{index}] dbType={0} expired={1} revoked={2} -> isAccessTokenValid={3}")
    @MethodSource("tokenStateCube")
    @DisplayName("isAccessTokenValid SHALL return false for any non-ACCESS or revoked/expired DB token")
    void isAccessTokenValid_acrossTokenStateCube(
            TokenType dbType,
            boolean dbExpired,
            boolean dbRevoked,
            boolean expectedValid) {

        // --- Arrange --------------------------------------------------------
        // Build a JWT signed with the SAME secret as JwtService so signature
        // verification succeeds. TTL matches the role of the persisted token,
        // so the JWT itself is not temporally expired (avoids confounding
        // BUG-1 with BUG-2 / generic expiration handling).
        long ttl = dbType == TokenType.REFRESH_TOKEN ? REFRESH_TTL_MS : ACCESS_TTL_MS;
        String jwt = buildSignedJwt(USERNAME, ttl);

        Token tokenDb = Token.builder()
                .token(jwt)
                .tokenType(dbType)
                .expired(dbExpired)
                .revoked(dbRevoked)
                .userId(null)
                .build();
        ReflectionTestUtils.setField(tokenDb, "id", UUID.randomUUID());

        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tokenDb));

        // --- Act ------------------------------------------------------------
        boolean actualValid = jwtService.isAccessTokenValid(jwt, userDetails);

        // --- Assert ---------------------------------------------------------
        // For the bug-condition row (REFRESH_TOKEN, expired=false, revoked=false)
        // unfixed code returns true; this assertion will fail there with
        // counterexample: dbType=REFRESH_TOKEN, dbExpired=false, dbRevoked=false
        // -> expected=false, actual=true.
        assertThat(actualValid)
                .as("isAccessTokenValid for dbType=%s, dbExpired=%s, dbRevoked=%s",
                        dbType, dbExpired, dbRevoked)
                .isEqualTo(expectedValid);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build a JWT signed with the same HMAC secret JwtService uses, so the
     * signature passes parsing in {@link JwtService#extractAllClaims(String)}.
     * Issued-at is now and expiration is now+{@code ttlMs}.
     */
    private static String buildSignedJwt(String subject, long ttlMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(new HashMap<>())
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMs))
                .signWith(signKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private static Key signKey() {
        byte[] keyBytes = Decoders.BASE64.decode(BASE64_SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
