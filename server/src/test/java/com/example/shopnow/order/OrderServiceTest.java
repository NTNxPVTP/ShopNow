package com.example.shopnow.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.repository.OrderRepository;
import com.example.shopnow.order.domain.repository.SubOrderRepository;
import com.example.shopnow.order.infrastructure.persistence.OrderJpaRepository;
import com.example.shopnow.order.infrastructure.persistence.SubOrderJpaRepository;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.models.SubOrder;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.product.api.ProductApi;
import com.example.shopnow.shared.PageInfo;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

/**
 * Unit tests cho {@link OrderService} — phần truy vấn và phân quyền.
 *
 * <p>Bao phủ:
 * <ul>
 *   <li>{@code getOrderDetail}: not-found, viewer ≠ customer, viewer = customer.</li>
 *   <li>{@code getOrders}: customer null / id null, valid.</li>
 *   <li>seller-guard cho {@code getSubOrderDetail}/{@code getSubOrders}: viewer null / role
 *       null / id null (parametrized), non-SELLER role, SELLER not-found, SELLER found,
 *       SELLER list.</li>
 * </ul>
 *
 * <p>Theo yêu cầu của test wave hiện tại, các kịch bản tạo đơn hàng (createOrder) và
 * tính tổng tiền sẽ được bổ sung trong task 3.2 vào cùng class này.
 *
 * <p>Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private OrderJpaRepository orderJpaRepository;
    @Mock
    private SubOrderJpaRepository subOrderJpaRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private SubOrderMapper subOrderMapper;
    @Mock
    private ProductApi productApi;

    @InjectMocks
    private OrderService orderService;

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /** Tạo Order với id và customerId được set qua reflection (BaseEntity.id là @GeneratedValue). */
    private static Order orderWithCustomer(UUID orderId, UUID customerId) {
        Order order = Order.builder().build();
        ReflectionTestUtils.setField(order, "id", orderId);
        order.setCustomerId(customerId);
        return order;
    }

    private static AuthenticatedUser sellerWithId(UUID id) {
        AuthenticatedUser viewer = Mockito.mock(AuthenticatedUser.class);
        when(viewer.getId()).thenReturn(id);
        when(viewer.getRole()).thenReturn("SELLER");
        return viewer;
    }

    private static AuthenticatedUser customerWithId(UUID id) {
        AuthenticatedUser viewer = Mockito.mock(AuthenticatedUser.class);
        org.mockito.Mockito.lenient().when(viewer.getId()).thenReturn(id);
        return viewer;
    }

    // ---------------------------------------------------------------------
    // getOrderDetail
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetail {

        @Test
        @DisplayName("ném ORDER_NOT_FOUND khi order không tồn tại")
        void notFound_throwsOrderNotFound() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            AuthenticatedUser viewer = customerWithId(UUID.randomUUID());
            when(orderRepository.findWithDetailById(orderId)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> orderService.getOrderDetail(orderId, viewer))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(orderMapper, never()).toDto(any(Order.class));
        }

        @Test
        @DisplayName("ném ORDER_ACCESS_DENIED khi viewer không phải customer của order")
        void viewerNotCustomer_throwsAccessDenied() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            UUID otherViewerId = UUID.randomUUID();
            Order order = orderWithCustomer(orderId, customerId);

            AuthenticatedUser viewer = customerWithId(otherViewerId);
            when(orderRepository.findWithDetailById(orderId)).thenReturn(Optional.of(order));

            // Act + Assert
            assertThatThrownBy(() -> orderService.getOrderDetail(orderId, viewer))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verify(orderMapper, never()).toDto(any(Order.class));
        }

        @Test
        @DisplayName("trả về OrderDTO khi viewer là customer của order")
        void viewerIsCustomer_returnsMappedDto() {
            // Arrange
            UUID orderId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();
            Order order = orderWithCustomer(orderId, customerId);

            AuthenticatedUser viewer = customerWithId(customerId);
            when(orderRepository.findWithDetailById(orderId)).thenReturn(Optional.of(order));

            OrderDTO expectedDto = new OrderDTO(
                    orderId, OrderStatus.IN_PROCESS, null,
                    null, null, null, List.of(), null);
            when(orderMapper.toDto(order)).thenReturn(expectedDto);

            // Act
            OrderDTO result = orderService.getOrderDetail(orderId, viewer);

            // Assert
            assertThat(result).isSameAs(expectedDto);
            verify(orderMapper).toDto(order);
        }
    }

    // ---------------------------------------------------------------------
    // getOrders
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("getOrders")
    class GetOrders {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("ném ORDER_ACCESS_DENIED khi customer null")
        void customerNull_throwsAccessDenied() {
            assertThatThrownBy(() -> orderService.getOrders(pageable, null, null, null))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(orderRepository, orderMapper);
        }

        @Test
        @DisplayName("ném ORDER_ACCESS_DENIED khi customer.getId() null")
        void customerIdNull_throwsAccessDenied() {
            AuthenticatedUser customer = Mockito.mock(AuthenticatedUser.class);
            when(customer.getId()).thenReturn(null);

            assertThatThrownBy(() -> orderService.getOrders(pageable, customer, null, null))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(orderRepository, orderMapper);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("trả về PageResponse từ orderMapper khi customer hợp lệ")
        void validCustomer_returnsMappedPageResponse() {
            // Arrange
            UUID customerId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            AuthenticatedUser customer = customerWithId(customerId);

            Page<Order> page = new PageImpl<>(List.of(orderWithCustomer(UUID.randomUUID(), customerId)),
                    pageable, 1);
            when(orderJpaRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            PageResponse<OrderSummaryDTO> expected = new PageResponse<>(
                    List.of(),
                    PageInfo.builder().pageNumber(0).pageSize(10).totalPages(1)
                            .isLast(true).totalElements(1L).build());
            when(orderMapper.toSummaryPageResponse(page)).thenReturn(expected);

            // Act
            PageResponse<OrderSummaryDTO> result = orderService.getOrders(
                    pageable, customer, OrderStatus.IN_PROCESS, shopId);

            // Assert
            assertThat(result).isSameAs(expected);
            verify(orderJpaRepository).findAll(any(Specification.class), eq(pageable));
            verify(orderMapper).toSummaryPageResponse(page);
        }
    }

    // ---------------------------------------------------------------------
    // Seller-guard cho getSubOrderDetail & getSubOrders
    // ---------------------------------------------------------------------

    /**
     * Cung cấp 3 biến thể "viewer" mà mọi seller-guard phải từ chối:
     * viewer null, role null, id null. Sử dụng cho cả {@code getSubOrderDetail}
     * và {@code getSubOrders}.
     */
    static java.util.stream.Stream<Arguments> nullViewerVariants() {
        // (label, supplier of viewer)
        return java.util.stream.Stream.of(
                Arguments.of("viewer == null",
                        (java.util.function.Supplier<AuthenticatedUser>) () -> null),
                Arguments.of("role == null",
                        (java.util.function.Supplier<AuthenticatedUser>) () -> {
                            AuthenticatedUser u = Mockito.mock(AuthenticatedUser.class);
                            when(u.getRole()).thenReturn(null);
                            // id may or may not be queried before role; do not stub strictly
                            return u;
                        }),
                Arguments.of("id == null",
                        (java.util.function.Supplier<AuthenticatedUser>) () -> {
                            AuthenticatedUser u = Mockito.mock(AuthenticatedUser.class);
                            org.mockito.Mockito.lenient().when(u.getRole()).thenReturn("SELLER");
                            when(u.getId()).thenReturn(null);
                            return u;
                        }));
    }

    @Nested
    @DisplayName("getSubOrderDetail seller-guard")
    class SubOrderDetailGuard {

        @ParameterizedTest(name = "ném ORDER_ACCESS_DENIED khi {0}")
        @MethodSource("com.example.shopnow.order.OrderServiceTest#nullViewerVariants")
        void nullViewerVariants_throwAccessDenied(
                String label, java.util.function.Supplier<AuthenticatedUser> viewerSupplier) {
            AuthenticatedUser viewer = viewerSupplier.get();

            assertThatThrownBy(() -> orderService.getSubOrderDetail(UUID.randomUUID(), viewer))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(subOrderRepository, subOrderMapper);
        }

        @Test
        @DisplayName("ném ORDER_ACCESS_DENIED khi role không phải SELLER")
        void nonSellerRole_throwsAccessDenied() {
            AuthenticatedUser viewer = Mockito.mock(AuthenticatedUser.class);
            when(viewer.getId()).thenReturn(UUID.randomUUID());
            when(viewer.getRole()).thenReturn("CUSTOMER");

            assertThatThrownBy(() -> orderService.getSubOrderDetail(UUID.randomUUID(), viewer))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(subOrderRepository, subOrderMapper);
        }

        @Test
        @DisplayName("SELLER nhưng sub-order không tồn tại → ORDER_NOT_FOUND")
        void seller_subOrderNotFound_throwsOrderNotFound() {
            UUID viewerId = UUID.randomUUID();
            UUID subOrderId = UUID.randomUUID();
            AuthenticatedUser viewer = sellerWithId(viewerId);

            when(subOrderRepository.findWithDetailByIdAndShopOwnerId(subOrderId, viewerId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getSubOrderDetail(subOrderId, viewer))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(subOrderMapper, never()).toDto(any(SubOrder.class));
        }

        @Test
        @DisplayName("SELLER và sub-order tồn tại → trả về SubOrderDTO")
        void seller_subOrderFound_returnsMappedDto() {
            UUID viewerId = UUID.randomUUID();
            UUID subOrderId = UUID.randomUUID();
            AuthenticatedUser viewer = sellerWithId(viewerId);

            SubOrder subOrder = SubOrder.builder().build();
            ReflectionTestUtils.setField(subOrder, "id", subOrderId);
            when(subOrderRepository.findWithDetailByIdAndShopOwnerId(subOrderId, viewerId))
                    .thenReturn(Optional.of(subOrder));

            SubOrderDTO expected = new SubOrderDTO(
                    subOrderId, UUID.randomUUID(), OrderStatus.IN_PROCESS,
                    null, List.of(), null, null);
            when(subOrderMapper.toDto(subOrder)).thenReturn(expected);

            SubOrderDTO result = orderService.getSubOrderDetail(subOrderId, viewer);

            assertThat(result).isSameAs(expected);
            verify(subOrderMapper).toDto(subOrder);
        }
    }

    @Nested
    @DisplayName("getSubOrders seller-guard")
    class SubOrdersGuard {

        private final Pageable pageable = PageRequest.of(0, 10);

        @ParameterizedTest(name = "ném ORDER_ACCESS_DENIED khi {0}")
        @MethodSource("com.example.shopnow.order.OrderServiceTest#nullViewerVariants")
        void nullViewerVariants_throwAccessDenied(
                String label, java.util.function.Supplier<AuthenticatedUser> viewerSupplier) {
            AuthenticatedUser viewer = viewerSupplier.get();

            assertThatThrownBy(() -> orderService.getSubOrders(pageable, viewer, null, null))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(subOrderRepository, subOrderMapper);
        }

        @Test
        @DisplayName("ném ORDER_ACCESS_DENIED khi role không phải SELLER")
        void nonSellerRole_throwsAccessDenied() {
            AuthenticatedUser viewer = Mockito.mock(AuthenticatedUser.class);
            when(viewer.getId()).thenReturn(UUID.randomUUID());
            when(viewer.getRole()).thenReturn("CUSTOMER");

            assertThatThrownBy(() -> orderService.getSubOrders(pageable, viewer, null, null))
                    .isInstanceOf(DomainException.class)
                    .extracting(ex -> ((DomainException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);

            verifyNoInteractions(subOrderRepository, subOrderMapper);
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("SELLER → trả về PageResponse từ subOrderMapper")
        void seller_returnsMappedPageResponse() {
            UUID viewerId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            AuthenticatedUser viewer = sellerWithId(viewerId);

            Page<SubOrder> page = new PageImpl<>(List.of(SubOrder.builder().build()), pageable, 1);
            when(subOrderJpaRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

            PageResponse<SubOrderSummaryDTO> expected = new PageResponse<>(
                    List.of(),
                    PageInfo.builder().pageNumber(0).pageSize(10).totalPages(1)
                            .isLast(true).totalElements(1L).build());
            when(subOrderMapper.toSummaryPageResponse(page)).thenReturn(expected);

            PageResponse<SubOrderSummaryDTO> result = orderService.getSubOrders(
                    pageable, viewer, OrderStatus.IN_PROCESS, shopId);

            assertThat(result).isSameAs(expected);
            verify(subOrderJpaRepository).findAll(any(Specification.class), eq(pageable));
            verify(subOrderMapper).toSummaryPageResponse(page);
        }
    }

    // ---------------------------------------------------------------------
    // createOrder (Requirements 3.11, 3.12, 3.13)
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("single-shop: tạo 1 sub-order, tính đúng sub-order total và grand total")
        void singleShop_oneSubOrder_correctTotals() {
            // Arrange
            UUID buyerId = UUID.randomUUID();
            UUID shopId = UUID.randomUUID();
            UUID shopOwnerId = UUID.randomUUID();
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();

            AuthenticatedUser buyer = customerWithId(buyerId);

            List<com.example.shopnow.order.rest.dto.OrderItemRequest> items = List.of(
                    new com.example.shopnow.order.rest.dto.OrderItemRequest(productId1, 2),
                    new com.example.shopnow.order.rest.dto.OrderItemRequest(productId2, 3));

            com.example.shopnow.order.rest.dto.CreateOrderRequest request =
                    new com.example.shopnow.order.rest.dto.CreateOrderRequest(
                            items, "123 Street", "0901234567", "Buyer Name");

            // productApi trả về thông tin sản phẩm
            List<com.example.shopnow.product.api.dto.ProductInfoForOrder> productInfos = List.of(
                    new com.example.shopnow.product.api.dto.ProductInfoForOrder(
                            productId1, new java.math.BigDecimal("10.00"), "P1", 100, shopId, shopOwnerId),
                    new com.example.shopnow.product.api.dto.ProductInfoForOrder(
                            productId2, new java.math.BigDecimal("20.00"), "P2", 100, shopId, shopOwnerId));
            when(productApi.decreaseProducts(any())).thenReturn(productInfos);
            when(orderMapper.fromRequestToOrder(request)).thenReturn(Order.builder().build());
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderDTO dummyDto = new OrderDTO(UUID.randomUUID(), OrderStatus.IN_PROCESS,
                    null, "123 Street", "0901234567", "Buyer Name", List.of(), null);
            when(orderMapper.toDto(any(Order.class))).thenReturn(dummyDto);

            // Act
            orderService.createOrder(request, buyer);

            // Assert: capture the saved Order
            org.mockito.ArgumentCaptor<Order> orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            // grand total = (10.00 * 2) + (20.00 * 3) = 20.00 + 60.00 = 80.00
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new java.math.BigDecimal("80.00"));
            assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.IN_PROCESS);
            assertThat(savedOrder.getCustomerId()).isEqualTo(buyerId);

            // 1 sub-order (single shop)
            assertThat(savedOrder.getSubOrders()).hasSize(1);
            SubOrder subOrder = savedOrder.getSubOrders().iterator().next();
            assertThat(subOrder.getShopId()).isEqualTo(shopId);
            assertThat(subOrder.getShopOwnerId()).isEqualTo(shopOwnerId);
            assertThat(subOrder.getStatus()).isEqualTo(OrderStatus.IN_PROCESS);
            assertThat(subOrder.getTotalPrice())
                    .isEqualByComparingTo(new java.math.BigDecimal("80.00"));

            // 2 order details
            assertThat(subOrder.getOrderDetails()).hasSize(2);
        }

        @Test
        @DisplayName("multi-shop: tạo nhiều sub-orders, parent total = tổng tất cả sub-order totals")
        void multiShop_multipleSubOrders_grandTotalIsSum() {
            // Arrange
            UUID buyerId = UUID.randomUUID();
            UUID shopA = UUID.randomUUID();
            UUID shopB = UUID.randomUUID();
            UUID ownerA = UUID.randomUUID();
            UUID ownerB = UUID.randomUUID();
            UUID prodA1 = UUID.randomUUID();
            UUID prodB1 = UUID.randomUUID();
            UUID prodB2 = UUID.randomUUID();

            AuthenticatedUser buyer = customerWithId(buyerId);

            List<com.example.shopnow.order.rest.dto.OrderItemRequest> items = List.of(
                    new com.example.shopnow.order.rest.dto.OrderItemRequest(prodA1, 1),  // shopA: 15.00 * 1
                    new com.example.shopnow.order.rest.dto.OrderItemRequest(prodB1, 2),  // shopB: 25.00 * 2
                    new com.example.shopnow.order.rest.dto.OrderItemRequest(prodB2, 4)); // shopB: 5.00 * 4

            com.example.shopnow.order.rest.dto.CreateOrderRequest request =
                    new com.example.shopnow.order.rest.dto.CreateOrderRequest(
                            items, "456 Avenue", "0987654321", "Multi Buyer");

            List<com.example.shopnow.product.api.dto.ProductInfoForOrder> productInfos = List.of(
                    new com.example.shopnow.product.api.dto.ProductInfoForOrder(
                            prodA1, new java.math.BigDecimal("15.00"), "PA1", 100, shopA, ownerA),
                    new com.example.shopnow.product.api.dto.ProductInfoForOrder(
                            prodB1, new java.math.BigDecimal("25.00"), "PB1", 100, shopB, ownerB),
                    new com.example.shopnow.product.api.dto.ProductInfoForOrder(
                            prodB2, new java.math.BigDecimal("5.00"), "PB2", 100, shopB, ownerB));
            when(productApi.decreaseProducts(any())).thenReturn(productInfos);
            when(orderMapper.fromRequestToOrder(request)).thenReturn(Order.builder().build());
            when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

            OrderDTO dummyDto = new OrderDTO(UUID.randomUUID(), OrderStatus.IN_PROCESS,
                    null, null, null, null, List.of(), null);
            when(orderMapper.toDto(any(Order.class))).thenReturn(dummyDto);

            // Act
            orderService.createOrder(request, buyer);

            // Assert
            org.mockito.ArgumentCaptor<Order> orderCaptor = org.mockito.ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            // shopA total = 15.00 * 1 = 15.00
            // shopB total = (25.00 * 2) + (5.00 * 4) = 50.00 + 20.00 = 70.00
            // grand total = 15.00 + 70.00 = 85.00
            assertThat(savedOrder.getTotalPrice())
                    .isEqualByComparingTo(new java.math.BigDecimal("85.00"));

            assertThat(savedOrder.getSubOrders()).hasSize(2);

            // Verify each sub-order total
            java.util.Map<UUID, SubOrder> subOrderByShop = new java.util.HashMap<>();
            for (SubOrder so : savedOrder.getSubOrders()) {
                subOrderByShop.put(so.getShopId(), so);
            }

            assertThat(subOrderByShop.get(shopA).getTotalPrice())
                    .isEqualByComparingTo(new java.math.BigDecimal("15.00"));
            assertThat(subOrderByShop.get(shopA).getShopOwnerId()).isEqualTo(ownerA);
            assertThat(subOrderByShop.get(shopA).getOrderDetails()).hasSize(1);

            assertThat(subOrderByShop.get(shopB).getTotalPrice())
                    .isEqualByComparingTo(new java.math.BigDecimal("70.00"));
            assertThat(subOrderByShop.get(shopB).getShopOwnerId()).isEqualTo(ownerB);
            assertThat(subOrderByShop.get(shopB).getOrderDetails()).hasSize(2);

            // Verify sum of sub-order totals equals grand total
            java.math.BigDecimal sumSubOrders = savedOrder.getSubOrders().stream()
                    .map(SubOrder::getTotalPrice)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            assertThat(sumSubOrders).isEqualByComparingTo(savedOrder.getTotalPrice());
        }
    }
}
