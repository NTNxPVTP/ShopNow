package com.example.shopnow.user.api;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Principal công khai của module User.
 *
 * <p>Kế thừa {@link UserDetails} để tương thích với Spring Security, nhưng chỉ
 * lộ dữ liệu cần thiết cho các module khác. Entity nội bộ {@code user.models.User}
 * hiện thực interface này, nên principal trong SecurityContext vẫn là instance
 * {@code User} ở runtime trong khi các module khác chỉ thấy hợp đồng công khai.
 */
public interface AuthenticatedUser extends UserDetails {

    UUID getId();

    String getEmail();

    /** Tên role dạng String (giá trị bằng {@code Role.name()}), không lộ enum nội bộ. */
    String getRole();
}
