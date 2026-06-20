package com.example.shopnow.user;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.shopnow.user.api.AuthenticatedUser;
import com.example.shopnow.user.api.UserApi;
import com.example.shopnow.user.application.usecases.ProvisionOAuthUserUseCase;

import lombok.RequiredArgsConstructor;

/**
 * Hiện thực nội bộ của {@link UserApi}.
 *
 * <p>Giữ {@link UserRepository} (và các collaborator nội bộ khác) ẩn sau Published API;
 * các module khác chỉ thấy {@link UserApi}/{@link AuthenticatedUser}.
 *
 * <p>User là module phức tạp vừa phải: chỉ {@code provisionOAuthUser} có
 * orchestration thực sự (get-or-create), nên hành vi đó được tách ra
 * {@link ProvisionOAuthUserUseCase}. {@code findByEmail} là tra cứu thuần,
 * không invariant/orchestration, nên giữ inline tại đây thay vì bọc trong use case.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserApi {
    private final UserRepository repository;
    private final ProvisionOAuthUserUseCase provisionOAuthUserUseCase;

    /**
     * Tra cứu người dùng theo email. KHÔNG ném khi không tìm thấy — trả
     * {@link Optional#empty()} để caller tự quyết định phản hồi.
     *
     * <p>Tra cứu thuần (không invariant/orchestration) nên không tách use case.
     */
    @Override
    public Optional<AuthenticatedUser> findByEmail(String email) {
        return repository.findByEmail(email).map(user -> (AuthenticatedUser) user);
    }

    /**
     * Provisioning người dùng cho luồng OAuth: tạo CUSTOMER với password placeholder
     * ngẫu nhiên nếu email chưa tồn tại, trả về người dùng hiện có nếu đã tồn tại.
     *
     * <p>Ủy quyền cho {@link ProvisionOAuthUserUseCase} — hành vi giữ nguyên
     * (no behavior change), chữ ký {@link UserApi} không đổi.
     */
    @Override
    public AuthenticatedUser provisionOAuthUser(String email, String name) {
        return provisionOAuthUserUseCase.execute(email, name);
    }
}
