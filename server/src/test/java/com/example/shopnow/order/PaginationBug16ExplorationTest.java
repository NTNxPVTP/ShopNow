package com.example.shopnow.order;

import com.example.shopnow.order.rest.OrderController;
import com.example.shopnow.order.rest.SubOrderController;
import com.example.shopnow.product.ProductServiceImpl;
import com.example.shopnow.product.rest.ProductController;
import com.example.shopnow.shared.PageInfo;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-16: page≤0 causes 500 (IllegalArgumentException).
 *
 * <p><b>Validates: Requirements 2.16</b>
 *
 * <p>Phase 1 bug-condition exploration test. EXPECTED TO FAIL on unfixed code.
 * The failure surfaces a counterexample proving that passing page=0 or page=-5
 * to paginated endpoints causes {@code IllegalArgumentException("Page index
 * must not be less than zero")} from {@code PageRequest.of(page-1, size, ...)}
 * which surfaces as HTTP 500.
 *
 * <p><b>Bug condition C(16):</b> page ∈ [-5, 0] passed to any paginated
 * endpoint (OrderController.getOrders, SubOrderController.getSubOrders,
 * ProductController.getProducts).
 *
 * <p><b>Property P(16):</b> for every input satisfying C(16), the controller
 * SHALL NOT throw {@code IllegalArgumentException}. It should either clamp
 * to page 1 (return 200) or return 400 validation error.
 *
 * <p><b>Test design:</b> JUnit 5 + Mockito (no Spring context). Controllers
 * are instantiated via {@code @InjectMocks}; service dependencies are mocked.
 * The service mocks are configured leniently since the bug fires before the
 * service is ever called (IllegalArgumentException from PageRequest.of).
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code page=0} → {@code PageRequest.of(-1, 10, ...)} →
 * {@code IllegalArgumentException("Page index must not be less than zero")}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaginationBug16ExplorationTest {

    // --- OrderController dependencies ---
    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    // --- SubOrderController dependencies ---
    // SubOrderController also depends on OrderService (same field name "service")
    @InjectMocks
    private SubOrderController subOrderController;

    // --- ProductController dependencies ---
    @Mock
    private ProductServiceImpl productService;

    @InjectMocks
    private ProductController productController;

    private User createViewer() {
        User viewer = User.builder()
                .name("testuser")
                .email("test@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();
        ReflectionTestUtils.setField(viewer, "id", UUID.randomUUID());
        return viewer;
    }

    /**
     * OrderController.getOrders with page≤0 must NOT throw
     * IllegalArgumentException. On unfixed code, PageRequest.of(page-1, 10, ...)
     * with page=0 yields PageRequest.of(-1, ...) which throws.
     */
    @ParameterizedTest(name = "OrderController.getOrders(page={0}) must not throw IllegalArgumentException")
    @ValueSource(ints = {0, -5})
    @DisplayName("BUG-16: OrderController.getOrders with page≤0 must NOT throw IllegalArgumentException")
    void orderController_getOrders_doesNotThrowForInvalidPage(int page) {
        User viewer = createViewer();

        // Stub service leniently - the bug fires before service is called
        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(1).pageSize(10).totalPages(0).totalElements(0).isLast(true).build();
        when(orderService.getOrders(any(Pageable.class), any(User.class), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), pageInfo));

        assertThatCode(() -> orderController.getOrers(page, null, null, viewer))
                .as("OrderController.getOrders(page=%d) must not throw "
                        + "IllegalArgumentException (should clamp or return 400)", page)
                .doesNotThrowAnyException();
    }

    /**
     * SubOrderController.getSubOrders with page≤0 must NOT throw
     * IllegalArgumentException. On unfixed code, PageRequest.of(page-1, 10, ...)
     * with page=0 yields PageRequest.of(-1, ...) which throws.
     */
    @ParameterizedTest(name = "SubOrderController.getSubOrders(page={0}) must not throw IllegalArgumentException")
    @ValueSource(ints = {0, -5})
    @DisplayName("BUG-16: SubOrderController.getSubOrders with page≤0 must NOT throw IllegalArgumentException")
    void subOrderController_getSubOrders_doesNotThrowForInvalidPage(int page) {
        User viewer = createViewer();

        // Stub service leniently
        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(1).pageSize(10).totalPages(0).totalElements(0).isLast(true).build();
        when(orderService.getSubOrders(any(Pageable.class), any(User.class), any(), any()))
                .thenReturn(new PageResponse<>(List.of(), pageInfo));

        assertThatCode(() -> subOrderController.getSubOrders(viewer, page, null, null))
                .as("SubOrderController.getSubOrders(page=%d) must not throw "
                        + "IllegalArgumentException (should clamp or return 400)", page)
                .doesNotThrowAnyException();
    }

    /**
     * ProductController.getProducts with page≤0 must NOT throw
     * IllegalArgumentException. On unfixed code, PageRequest.of(page-1, size, ...)
     * with page=0 yields PageRequest.of(-1, ...) which throws.
     */
    @ParameterizedTest(name = "ProductController.getProducts(page={0}) must not throw IllegalArgumentException")
    @ValueSource(ints = {0, -5})
    @DisplayName("BUG-16: ProductController.getProducts with page≤0 must NOT throw IllegalArgumentException")
    void productController_getProducts_doesNotThrowForInvalidPage(int page) {
        // Stub service leniently
        PageInfo pageInfo = PageInfo.builder()
                .pageNumber(1).pageSize(10).totalPages(0).totalElements(0).isLast(true).build();
        when(productService.getProducts(any(Pageable.class), any(), any(), any(), any(), any(), eq(false)))
                .thenReturn(new PageResponse<>(List.of(), pageInfo));

        assertThatCode(() -> productController.getProducts(page, 10, null, null, null, null, null, false))
                .as("ProductController.getProducts(page=%d) must not throw "
                        + "IllegalArgumentException (should clamp or return 400)", page)
                .doesNotThrowAnyException();
    }
}
