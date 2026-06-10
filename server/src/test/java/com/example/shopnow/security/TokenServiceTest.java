package com.example.shopnow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.security.models.Token;
import com.example.shopnow.security.models.TokenType;
import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;

/**
 * Unit test cho {@link TokenService}.
 *
 * <p>Bao phủ các nhánh thu hồi/lưu token:
 * <ul>
 *   <li>{@link TokenService#revokeAllUserTokens(AuthenticatedUser)} — không có valid token
 *       (KHÔNG gọi {@code saveAll}) và có valid token (mỗi token bị đánh dấu
 *       {@code expired=true} & {@code revoked=true}, sau đó {@code saveAll} được gọi
 *       với danh sách đã cập nhật).</li>
 *   <li>{@link TokenService#revokeAllUserTokens(String)} — email resolve được
 *       (delegate sang overload {@code AuthenticatedUser}) và email không resolve
 *       (không tương tác với {@link TokenRepository}).</li>
 *   <li>{@link TokenService#saveUserTokens(AuthenticatedUser, String, String)} —
 *       gọi {@code save} đúng hai lần, captured arguments gồm một
 *       {@link TokenType#ACCESS_TOKEN} và một {@link TokenType#REFRESH_TOKEN};
 *       cả hai có {@code expired=false}, {@code revoked=false}, và cùng
 *       {@code userId} với user truyền vào.</li>
 * </ul>
 *
 * <p>Pure unit test theo convention của repo: JUnit 5 + Mockito + AssertJ, không
 * khởi tạo Spring context. {@link AuthenticatedUser} được mock và chỉ stub
 * {@code getId()} vì đó là dữ liệu duy nhất {@link TokenService} đọc.
 *
 * <p>Validates: Requirements 5.9, 5.10, 5.11, 5.12
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private UserApi userApi;

    @InjectMocks
    private TokenService tokenService;

    // ------------------------------------------------------------------
    // revokeAllUserTokens(AuthenticatedUser)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("revokeAllUserTokens(AuthenticatedUser)")
    class RevokeByUser {

        @Test
        @DisplayName("không có valid token: KHÔNG gọi saveAll")
        void revokeAllUserTokens_user_noValidTokens_neverCallsSaveAll() {
            // Arrange
            UUID userId = UUID.randomUUID();
            AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
            when(user.getId()).thenReturn(userId);
            when(tokenRepository.findAllValidTokenByUser(userId)).thenReturn(List.of());

            // Act
            tokenService.revokeAllUserTokens(user);

            // Assert
            verify(tokenRepository).findAllValidTokenByUser(userId);
            verify(tokenRepository, never()).saveAll(any());

            // Validates: Requirements 5.9
        }

        @Test
        @DisplayName("có valid tokens: mỗi token bị đánh dấu expired+revoked và saveAll được gọi với danh sách đó")
        void revokeAllUserTokens_user_withValidTokens_marksExpiredAndRevoked_andSavesAll() {
            // Arrange
            UUID userId = UUID.randomUUID();
            AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
            when(user.getId()).thenReturn(userId);

            Token t1 = Token.builder()
                    .token("acc-1")
                    .tokenType(TokenType.ACCESS_TOKEN)
                    .expired(false)
                    .revoked(false)
                    .userId(userId)
                    .build();
            Token t2 = Token.builder()
                    .token("ref-1")
                    .tokenType(TokenType.REFRESH_TOKEN)
                    .expired(false)
                    .revoked(false)
                    .userId(userId)
                    .build();
            ReflectionTestUtils.setField(t1, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(t2, "id", UUID.randomUUID());

            // Trả về một list mutable để service có thể mutate trong forEach
            List<Token> validTokens = new java.util.ArrayList<>(List.of(t1, t2));
            when(tokenRepository.findAllValidTokenByUser(userId)).thenReturn(validTokens);

            // Act
            tokenService.revokeAllUserTokens(user);

            // Assert: capture saveAll argument
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Iterable<Token>> captor = ArgumentCaptor.forClass(Iterable.class);
            verify(tokenRepository).saveAll(captor.capture());

            List<Token> saved = new java.util.ArrayList<>();
            captor.getValue().forEach(saved::add);

            assertThat(saved)
                    .as("saveAll phải nhận đúng danh sách valid tokens đã tra cứu")
                    .containsExactlyElementsOf(validTokens);
            assertThat(saved)
                    .as("Mỗi token sau khi revoke phải có expired=true và revoked=true")
                    .allSatisfy(token -> {
                        assertThat(token.isExpired()).isTrue();
                        assertThat(token.isRevoked()).isTrue();
                    });

            // Validates: Requirements 5.10
        }
    }

    // ------------------------------------------------------------------
    // revokeAllUserTokens(String email)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("revokeAllUserTokens(String email)")
    class RevokeByEmail {

        @Test
        @DisplayName("email resolve được: delegate sang overload AuthenticatedUser và saveAll được gọi")
        void revokeAllUserTokens_email_resolves_delegatesToUserOverload() {
            // Arrange
            String email = "alice@example.com";
            UUID userId = UUID.randomUUID();
            AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
            when(user.getId()).thenReturn(userId);

            when(userApi.findByEmail(email)).thenReturn(Optional.of(user));

            Token existing = Token.builder()
                    .token("acc-1")
                    .tokenType(TokenType.ACCESS_TOKEN)
                    .expired(false)
                    .revoked(false)
                    .userId(userId)
                    .build();
            ReflectionTestUtils.setField(existing, "id", UUID.randomUUID());
            List<Token> validTokens = new java.util.ArrayList<>(List.of(existing));
            when(tokenRepository.findAllValidTokenByUser(userId)).thenReturn(validTokens);

            // Act
            tokenService.revokeAllUserTokens(email);

            // Assert: delegate xảy ra → repo bị query và saveAll được gọi
            verify(userApi).findByEmail(email);
            verify(tokenRepository).findAllValidTokenByUser(userId);
            verify(tokenRepository).saveAll(validTokens);
            assertThat(existing.isExpired()).isTrue();
            assertThat(existing.isRevoked()).isTrue();

            // Validates: Requirements 5.11
        }

        @Test
        @DisplayName("email không resolve: KHÔNG tương tác TokenRepository")
        void revokeAllUserTokens_email_unresolved_noRepoInteraction() {
            // Arrange
            String email = "missing@example.com";
            when(userApi.findByEmail(email)).thenReturn(Optional.empty());

            // Act
            tokenService.revokeAllUserTokens(email);

            // Assert
            verify(userApi).findByEmail(email);
            verifyNoInteractions(tokenRepository);

            // Validates: Requirements 5.11
        }
    }

    // ------------------------------------------------------------------
    // saveUserTokens
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("saveUserTokens")
    class SaveUserTokens {

        @Test
        @DisplayName("gọi save đúng 2 lần với 1 ACCESS_TOKEN và 1 REFRESH_TOKEN, cả hai non-expired/non-revoked, đúng userId")
        void saveUserTokens_savesAccessAndRefreshTokens_withCorrectFlagsAndUserId() {
            // Arrange
            UUID userId = UUID.randomUUID();
            AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
            when(user.getId()).thenReturn(userId);

            String accessJwt = "acc-jwt-value";
            String refreshJwt = "ref-jwt-value";

            // Act
            tokenService.saveUserTokens(user, accessJwt, refreshJwt);

            // Assert: save được gọi đúng 2 lần với captured Token args
            ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);
            verify(tokenRepository, times(2)).save(captor.capture());
            // saveAll/findByToken không được dùng trong saveUserTokens
            verify(tokenRepository, never()).saveAll(any());
            verify(tokenRepository, never()).findByToken(anyString());

            List<Token> saved = captor.getAllValues();
            assertThat(saved).hasSize(2);

            // Đảm bảo có đúng 1 ACCESS_TOKEN và 1 REFRESH_TOKEN
            Token access = saved.stream()
                    .filter(t -> t.getTokenType() == TokenType.ACCESS_TOKEN)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Thiếu Token với type=ACCESS_TOKEN"));
            Token refresh = saved.stream()
                    .filter(t -> t.getTokenType() == TokenType.REFRESH_TOKEN)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Thiếu Token với type=REFRESH_TOKEN"));

            // ACCESS token mang đúng giá trị JWT, cờ mặc định, userId khớp
            assertThat(access.getToken()).isEqualTo(accessJwt);
            assertThat(access.isExpired()).isFalse();
            assertThat(access.isRevoked()).isFalse();
            assertThat(access.getUserId()).isEqualTo(userId);

            // REFRESH token mang đúng giá trị refresh, cờ mặc định, userId khớp
            assertThat(refresh.getToken()).isEqualTo(refreshJwt);
            assertThat(refresh.isExpired()).isFalse();
            assertThat(refresh.isRevoked()).isFalse();
            assertThat(refresh.getUserId()).isEqualTo(userId);

            // Validates: Requirements 5.12
        }
    }
}
