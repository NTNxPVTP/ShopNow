package com.example.shopnow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test cho {@link CartService}.
 *
 * <p>Pure unit test (JUnit 5 + Mockito + AssertJ), không khởi động Spring context,
 * theo đúng convention hiện có (xem {@code UserServiceImplTest}).
 *
 * <p>Encode hành vi placeholder hiện tại: {@code updateProductQuantity} luôn trả về
 * chuỗi {@code "update successfully"} và KHÔNG tương tác với {@code CartRepository}
 * (phần truy vấn repository đang bị comment trong production code). Test cố tình
 * khẳng định hành vi hiện tại này để làm regression baseline; khi production code
 * được hoàn thiện trong tương lai, test sẽ thất bại và cần cập nhật có chủ đích.
 *
 * <p>Validates: Requirements 4.5
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    @DisplayName("updateProductQuantity trả về 'update successfully' và không gọi CartRepository")
    void updateProductQuantity_returnsPlaceholder_andDoesNotTouchRepository() {
        // Act
        String result = cartService.updateProductQuantity("any-product", 5);

        // Assert: trả về chuỗi placeholder hiện tại
        assertThat(result).isEqualTo("update successfully");

        // Assert: repository không bị tương tác (hành vi placeholder hiện tại)
        verifyNoInteractions(cartRepository);
    }
}
