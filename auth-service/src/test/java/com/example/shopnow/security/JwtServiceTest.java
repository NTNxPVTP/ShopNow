package com.example.shopnow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.security.models.Token;
import com.example.shopnow.security.models.TokenType;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Unit test cho {@link JwtService}.
 *
 * <p>Bao phủ các kịch bản sinh/giải mã JWT và kiểm tra hợp lệ của access token
 * theo các nhánh: token null, không có trong DB, DB record bị expired, và
 * trường hợp hợp lệ (DB token thuộc loại ACCESS_TOKEN, không expired, không
 * revoked, username khớp). Đồng thời có một test mô tả "precedence quirk"
 * hiện tại của biểu thức trong {@code isTokenvalid} (xem chú thích trong test).
 *
 * <p>Pure unit test theo convention của repo: JUnit 5 + Mockito + AssertJ,
 * không khởi tạo Spring context. {@code @Value}-injected fields được thiết
 * lập qua {@link ReflectionTestUtils} trong {@link BeforeEach}.
 *
 * <p>Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8
 */
class JwtServiceTest {

    /** 256-bit HS256 secret (Base64-encoded). Đủ độ dài để jjwt chấp nhận. */
    private static final String BASE64_SECRET =
            "c2hvcG5vd190ZXN0X3NlY3JldF9rZXlfMzJfYnl0ZXNfbG9uZw==";

    /** TTL dương cho access token, đủ lớn để token còn hạn trong suốt test. */
    private static final long ACCESS_TTL_MS = 86_400_000L;

    /** TTL dương cho refresh token, đủ lớn để token còn hạn trong suốt test. */
    private static final long REFRESH_TTL_MS = 604_800_000L;

    private static final String USERNAME = "alice@example.com";
    private static final String OTHER_USERNAME = "bob@example.com";

    private TokenRepository tokenRepository;
    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        tokenRepository = Mockito.mock(TokenRepository.class);
        jwtService = new JwtService(tokenRepository);
        // @Value-injected fields phải được set thủ công vì test này không
        // khởi tạo Spring context.
        ReflectionTestUtils.setField(jwtService, "secretKey", BASE64_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", ACCESS_TTL_MS);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshExpiration", REFRESH_TTL_MS);

        userDetails = new User(USERNAME, "irrelevant", Collections.emptyList());
    }

    // ------------------------------------------------------------------
    // generateToken / extractUsername / isTokenValid
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("generateToken / extractUsername / isTokenValid")
    class GenerateAndExtract {

        @Test
        @DisplayName("generateToken: subject của JWT bằng username của UserDetails")
        void generateToken_subjectEqualsUsername() {
            // Act
            String jwt = jwtService.generateToken(userDetails);

            // Assert: subject (username) extract ra khớp với UserDetails
            String extracted = jwtService.extractUsername(jwt);
            assertThat(extracted).isEqualTo(USERNAME);

            // Validates: Requirements 5.1, 5.2
        }

        @Test
        @DisplayName("isTokenValid: trả true khi username khớp và token chưa hết hạn")
        void isTokenValid_returnsTrue_whenUsernameMatches_andNotExpired() {
            // Arrange
            String jwt = jwtService.generateToken(userDetails);

            // Act + Assert
            assertThat(jwtService.isTokenValid(jwt, userDetails)).isTrue();

            // Validates: Requirements 5.3
        }

        @Test
        @DisplayName("isTokenValid: trả false khi token được sinh cho username khác")
        void isTokenValid_returnsFalse_whenUsernameDiffers() {
            // Arrange: token sinh cho USERNAME nhưng kiểm với UserDetails của OTHER_USERNAME
            String jwt = jwtService.generateToken(userDetails);
            UserDetails otherUser = new User(OTHER_USERNAME, "irrelevant", Collections.emptyList());

            // Act + Assert
            assertThat(jwtService.isTokenValid(jwt, otherUser)).isFalse();

            // Validates: Requirements 5.4
        }
    }

    // ------------------------------------------------------------------
    // isAccessTokenValid
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("isAccessTokenValid")
    class IsAccessTokenValid {

        @Test
        @DisplayName("null token: trả false và KHÔNG truy vấn TokenRepository")
        void isAccessTokenValid_returnsFalse_andNeverQueriesRepo_whenTokenIsNull() {
            // Act
            boolean valid = jwtService.isAccessTokenValid(null, userDetails);

            // Assert
            assertThat(valid).isFalse();
            verify(tokenRepository, never()).findByToken(any());

            // Validates: Requirements 5.5
        }

        @Test
        @DisplayName("token không có trong DB: trả false")
        void isAccessTokenValid_returnsFalse_whenTokenAbsentInDb() {
            // Arrange
            String jwt = jwtService.generateToken(userDetails);
            when(tokenRepository.findByToken(jwt)).thenReturn(Optional.empty());

            // Act + Assert
            assertThat(jwtService.isAccessTokenValid(jwt, userDetails)).isFalse();

            // Validates: Requirements 5.6
        }

        @Test
        @DisplayName("DB record đã expired: trả false bất kể type/revoked")
        void isAccessTokenValid_returnsFalse_whenDbRecordExpired() {
            // Arrange
            String jwt = jwtService.generateToken(userDetails);
            Token tokenDb = Token.builder()
                    .token(jwt)
                    .tokenType(TokenType.ACCESS_TOKEN)
                    .expired(true)   // expired -> phải bị reject
                    .revoked(false)
                    .userId(UUID.randomUUID())
                    .build();
            ReflectionTestUtils.setField(tokenDb, "id", UUID.randomUUID());
            when(tokenRepository.findByToken(jwt)).thenReturn(Optional.of(tokenDb));

            // Act + Assert
            assertThat(jwtService.isAccessTokenValid(jwt, userDetails)).isFalse();

            // Validates: Requirements 5.7
        }

        @Test
        @DisplayName("DB record ACCESS_TOKEN, không expired, không revoked, username khớp: trả true")
        void isAccessTokenValid_returnsTrue_whenDbTokenValid_andUserMatches() {
            // Arrange
            String jwt = jwtService.generateToken(userDetails);
            Token tokenDb = Token.builder()
                    .token(jwt)
                    .tokenType(TokenType.ACCESS_TOKEN)
                    .expired(false)
                    .revoked(false)
                    .userId(UUID.randomUUID())
                    .build();
            ReflectionTestUtils.setField(tokenDb, "id", UUID.randomUUID());
            when(tokenRepository.findByToken(jwt)).thenReturn(Optional.of(tokenDb));

            // Act + Assert
            assertThat(jwtService.isAccessTokenValid(jwt, userDetails)).isTrue();

            // Validates: Requirements 5.8
        }
    }

    // ------------------------------------------------------------------
    // Precedence quirk - khoá đặc tính hiện tại của biểu thức trong isTokenvalid
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Precedence quirk in isTokenvalid (current behavior)")
    class PrecedenceQuirk {

        /**
         * Biểu thức early-return trong {@link JwtService} là:
         * <pre>
         *   tokenDB == null
         *     || tokenDB.isExpired()
         *     || tokenDB.isRevoked() &amp;&amp; tokenDB.getTokenType() == tokenType
         * </pre>
         * Trong Java, {@code &&} có độ ưu tiên cao hơn {@code ||}, nên biểu thức
         * tương đương với:
         * <pre>
         *   tokenDB == null
         *     || tokenDB.isExpired()
         *     || (tokenDB.isRevoked() &amp;&amp; tokenDB.getTokenType() == tokenType)
         * </pre>
         *
         * <p>Hệ quả là: một bản ghi có {@code revoked=true} nhưng
         * {@code tokenType=REFRESH_TOKEN} (khác với {@code ACCESS_TOKEN} đang
         * kiểm) sẽ KHÔNG kích hoạt early-return; nếu chữ ký JWT hợp lệ và
         * username khớp thì hàm trả {@code true}. Test này khoá lại hành vi
         * hiện tại đó (không cố "sửa" trong test); một future fix biểu thức
         * sẽ làm test này thất bại để được cập nhật một cách có chủ đích.
         *
         * <p>Lưu ý: đây không phải là kết quả mong muốn về mặt bảo mật, nhưng
         * là hành vi hiện tại của code production và spec yêu cầu KHÔNG sửa
         * production. Đây là baseline để regression-detect khi fix sau này.
         */
        @Test
        @DisplayName("revoked REFRESH_TOKEN dùng làm access token: trả true do precedence quirk")
        void isAccessTokenValid_returnsTrue_forRevokedRefreshToken_dueToPrecedenceQuirk() {
            // Arrange: build a JWT có chữ ký hợp lệ và username khớp với userDetails
            String jwt = buildSignedJwt(USERNAME, ACCESS_TTL_MS);
            Token tokenDb = Token.builder()
                    .token(jwt)
                    .tokenType(TokenType.REFRESH_TOKEN) // KHÁC ACCESS_TOKEN đang kiểm
                    .expired(false)
                    .revoked(true)                      // bị revoke
                    .userId(UUID.randomUUID())
                    .build();
            ReflectionTestUtils.setField(tokenDb, "id", UUID.randomUUID());
            when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(tokenDb));

            // Act
            boolean valid = jwtService.isAccessTokenValid(jwt, userDetails);

            // Assert: do precedence quirk, vế (revoked && type==tokenType) là
            // (true && (REFRESH_TOKEN==ACCESS_TOKEN)) == false, nên early-return
            // KHÔNG kích hoạt; sau đó username/expiry hợp lệ -> hàm trả true.
            assertThat(valid)
                    .as("Hành vi hiện tại do precedence trong isTokenvalid: revoked REFRESH_TOKEN "
                            + "vẫn được chấp nhận khi kiểm bằng isAccessTokenValid. "
                            + "Test này khoá lại hành vi hiện tại; nếu sau này biểu thức được sửa, "
                            + "test sẽ fail và cần được cập nhật một cách có chủ đích.")
                    .isTrue();
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Build một JWT ký bằng cùng HMAC secret mà {@link JwtService} dùng, để
     * chữ ký vượt qua bước parse trong {@code extractAllClaims}.
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
