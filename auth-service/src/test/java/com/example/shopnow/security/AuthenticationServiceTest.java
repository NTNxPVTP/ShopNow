package com.example.shopnow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.example.shopnow.security.rest.dto.AuthenticationRequest;
import com.example.shopnow.security.rest.dto.AuthenticationResponse;
import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;

/**
 * Unit test cho {@link AuthenticationService#authenticate(AuthenticationRequest)}.
 *
 * <p>Xác minh luồng đăng nhập: gọi {@link AuthenticationManager}, tra cứu user,
 * thu hồi token cũ, lưu token mới và trả về {@link AuthenticationResponse} mang
 * access/refresh token. Đồng thời xác minh hành vi hiện tại khi {@code findByEmail}
 * trả về rỗng (propagate {@link NoSuchElementException} từ {@code orElseThrow()}).
 *
 * <p>Thiết kế KHÔNG dùng Property-Based Testing; đây là service-orchestration class
 * nên dùng unit test với JUnit 5 + Mockito + AssertJ.
 *
 * <p>Validates: Requirements 5.13, 5.14
 */
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserApi userService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("authenticate: trả về access/refresh token và thực thi đúng thứ tự authenticate → revokeAllUserTokens → saveUserTokens")
    void authenticate_success_returnsTokens_andInvokesCollaboratorsInOrder() {
        // Arrange
        String email = "user@example.com";
        String password = "secret";
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(email)
                .password(password)
                .build();

        AuthenticatedUser user = Mockito.mock(AuthenticatedUser.class);
        Authentication authentication = Mockito.mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.findByEmail(email)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("acc");
        when(jwtService.generateRefreshToken(user)).thenReturn("ref");

        // Act
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Assert: response mang đúng access/refresh token
        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("acc");
        assertThat(response.refreshToken()).isEqualTo("ref");

        // Assert: thứ tự tương tác authenticate → revokeAllUserTokens(user) → saveUserTokens(user,"acc","ref")
        InOrder inOrder = Mockito.inOrder(authenticationManager, tokenService);
        inOrder.verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        inOrder.verify(tokenService).revokeAllUserTokens(user);
        inOrder.verify(tokenService).saveUserTokens(user, "acc", "ref");
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("authenticate: propagate NoSuchElementException khi findByEmail rỗng và KHÔNG lưu token")
    void authenticate_propagatesException_whenUserNotFound() {
        // Arrange
        String email = "missing@example.com";
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email(email)
                .password("secret")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(Mockito.mock(Authentication.class));
        when(userService.findByEmail(email)).thenReturn(Optional.empty());

        // Act + Assert: orElseThrow() ném NoSuchElementException (hành vi hiện tại)
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(NoSuchElementException.class);

        // Assert: không token nào được lưu
        verify(tokenService, never()).saveUserTokens(any(), any(), any());
    }
}
