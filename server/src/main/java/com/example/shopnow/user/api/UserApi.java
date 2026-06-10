package com.example.shopnow.user.api;

import java.util.Optional;

/**
 * Published API của module User.
 *
 * <p>Cung cấp các thao tác tra cứu và provisioning người dùng cho các module khác
 * (ví dụ Security) mà không để lộ entity/repository nội bộ.
 */
public interface UserApi {

    /**
     * Tra cứu người dùng theo email.
     *
     * @return {@link Optional} chứa principal công khai nếu tồn tại; rỗng nếu không
     *         tìm thấy. KHÔNG ném ngoại lệ khi không có user, để module gọi tự quyết
     *         định phản hồi.
     */
    Optional<AuthenticatedUser> findByEmail(String email);

    /**
     * Provisioning người dùng cho luồng OAuth: tạo mới nếu chưa tồn tại, trả về
     * người dùng hiện có nếu đã tồn tại.
     *
     * @return principal công khai tương ứng với email.
     */
    AuthenticatedUser provisionOAuthUser(String email, String name);
}
