package com.example.shopnow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.application.usecases.ProvisionOAuthUserUseCase;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;

/**
 * Unit test cho {@link UserServiceImpl#provisionOAuthUser(String, String)}.
 *
 * <p>Xác minh logic provisioning OAuth được di chuyển nguyên trạng (no behavior change):
 * tạo mới CUSTOMER khi email chưa tồn tại, và trả về người dùng hiện có khi đã tồn tại.
 *
 * <p>Đây là refactor cấu trúc — thiết kế KHÔNG dùng Property-Based Testing; dùng unit test
 * với JUnit 5 + Mockito.
 *
 * <p>Validates: Requirements 5.6
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository repository;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // provisionOAuthUser delegates to the use case, which orchestrates the
        // same get-or-create logic against the (mocked) repository.
        userService = new UserServiceImpl(repository, new ProvisionOAuthUserUseCase(repository));
    }

    @Test
    @DisplayName("provisionOAuthUser tạo mới CUSTOMER khi email chưa tồn tại")
    void provisionOAuthUser_createsNewCustomer_whenEmailNotFound() {
        // Arrange
        String email = "newuser@example.com";
        String name = "New User";
        when(repository.findByEmail(email)).thenReturn(Optional.empty());
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AuthenticatedUser result = userService.provisionOAuthUser(email, name);

        // Assert: repository.save được gọi đúng một lần với user mới
        ArgumentCaptor<User> savedCaptor = ArgumentCaptor.forClass(User.class);
        verify(repository, times(1)).save(savedCaptor.capture());

        User savedUser = savedCaptor.getValue();
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getName()).isEqualTo(name);
        assertThat(savedUser.getRole()).isEqualTo(Role.CUSTOMER.name());
        // password placeholder ngẫu nhiên phải được sinh ra (khác null/rỗng)
        assertThat(savedUser.getPassword()).isNotBlank();

        // Giá trị trả về khớp với user được lưu
        assertThat(result).isSameAs(savedUser);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER.name());
    }

    @Test
    @DisplayName("provisionOAuthUser trả user hiện có và KHÔNG lưu khi email đã tồn tại")
    void provisionOAuthUser_returnsExistingUser_whenEmailExists() {
        // Arrange
        String email = "existing@example.com";
        User existing = User.builder()
                .email(email)
                .name("Existing User")
                .role(Role.SELLER)
                .password("already-hashed")
                .build();
        when(repository.findByEmail(email)).thenReturn(Optional.of(existing));

        // Act
        AuthenticatedUser result = userService.provisionOAuthUser(email, "Ignored Name");

        // Assert: trả về user hiện có, không tạo/lưu user mới
        assertThat(result).isSameAs(existing);
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getRole()).isEqualTo(Role.SELLER.name());
        verify(repository, never()).save(any(User.class));
    }
}
