package com.example.shopnow.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;

import lombok.RequiredArgsConstructor;

/**
 * Hiện thực nội bộ của {@link UserApi}.
 *
 * <p>Giữ {@link UserRepository} (và các collaborator nội bộ khác) ẩn sau Published API;
 * các module khác chỉ thấy {@link UserApi}/{@link AuthenticatedUser}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserApi {
    private final UserRepository repository;

    /**
     * Tra cứu người dùng theo email. KHÔNG ném khi không tìm thấy — trả
     * {@link Optional#empty()} để caller tự quyết định phản hồi.
     */
    @Override
    public Optional<AuthenticatedUser> findByEmail(String email) {
        return repository.findByEmail(email).map(user -> (AuthenticatedUser) user);
    }

    /**
     * Provisioning người dùng cho luồng OAuth: tạo CUSTOMER với password placeholder
     * ngẫu nhiên nếu email chưa tồn tại, trả về người dùng hiện có nếu đã tồn tại.
     *
     * <p>Logic được di chuyển nguyên trạng từ {@code OAuth2AuthenticationSuccessHandler}
     * — giữ đúng hành vi cũ (no behavior change).
     */
    @Override
    public AuthenticatedUser provisionOAuthUser(String email, String name) {
        return repository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .email(email)
                    .name(name)
                    .role(Role.CUSTOMER) // Default role for OAuth users
                    .password(UUID.randomUUID().toString()) // random placeholder password
                    .build();
            return repository.save(newUser);
        });
    }
}
