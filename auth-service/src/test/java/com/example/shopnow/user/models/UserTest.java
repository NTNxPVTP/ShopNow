package com.example.shopnow.user.models;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit test cho domain invariant của {@link User}.
 *
 * <p>Xác minh factory {@link User#createOAuthCustomer(String, String, String)}
 * luôn gán role hợp lệ {@link Role#CUSTOMER} và bảo toàn email/name/password
 * đúng như đầu vào (no behavior change cho luồng provisioning OAuth).
 *
 * <p>Pure unit test (JUnit 5 + AssertJ), không khởi tạo Spring context.
 *
 * <p>Validates: Requirements 8.1
 */
class UserTest {

    @Test
    @DisplayName("createOAuthCustomer luôn gán role CUSTOMER và giữ nguyên các field đầu vào")
    void createOAuthCustomer_setsCustomerRole_andPreservesFields() {
        // Arrange
        String email = "oauth@example.com";
        String name = "OAuth User";
        String placeholderPassword = "random-placeholder";

        // Act
        User user = User.createOAuthCustomer(email, name, placeholderPassword);

        // Assert
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getName()).isEqualTo(name);
        assertThat(user.getPassword()).isEqualTo(placeholderPassword);
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER.name());
    }
}
