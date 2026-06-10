package com.example.shopnow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;

/**
 * Unit test cho {@link UserServiceImpl#findByEmail(String)}.
 *
 * <p>Bổ sung coverage cho nhánh tra cứu theo email mà {@code UserServiceImplTest}
 * chưa bao phủ (lớp đó chỉ test {@code provisionOAuthUser}). Không tái hiện
 * {@code provisionOAuthUser} ở đây (Requirement 7.6).
 *
 * <p>Pure unit test theo convention của repo: JUnit 5 + Mockito + AssertJ,
 * không khởi tạo Spring context.
 *
 * <p>Validates: Requirements 4.1, 4.2
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplFindByEmailTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("findByEmail trả Optional chứa user dưới dạng AuthenticatedUser khi email tồn tại")
    void findByEmail_returnsUserAsAuthenticatedUser_whenEmailExists() {
        // Arrange
        String email = "existing@example.com";
        User user = User.builder()
                .email(email)
                .name("Existing User")
                .role(Role.CUSTOMER)
                .password("hashed-password")
                .build();
        when(repository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        Optional<AuthenticatedUser> result = userService.findByEmail(email);

        // Assert: Optional chứa đúng instance user, đã được nhìn qua hợp đồng AuthenticatedUser
        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(user);
        assertThat(result.get()).isInstanceOf(AuthenticatedUser.class);
        assertThat(result.get().getEmail()).isEqualTo(email);
        assertThat(result.get().getRole()).isEqualTo(Role.CUSTOMER.name());
    }

    @Test
    @DisplayName("findByEmail trả Optional.empty() khi không có user nào với email đó")
    void findByEmail_returnsEmpty_whenEmailAbsent() {
        // Arrange
        String email = "missing@example.com";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());

        // Act
        Optional<AuthenticatedUser> result = userService.findByEmail(email);

        // Assert
        assertThat(result).isEmpty();
    }
}
