package com.example.shopnow.order;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.product.ProductServiceImpl;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Exploration test for BUG-14 ({@link OrderService#getOrderDetail}
 * unconditionally rejects every viewer whose {@code id} is not equal to
 * {@code order.customerId} via
 * {@code if (!order.getCustomerId().equals(viewer.getId())) throw
 * ORDER_ACCESS_DENIED}, with no bypass for {@link Role#ADMIN}).
 *
 * <p><b>Validates: Requirements 2.14</b> (Property 14 in design.md).
 *
 * <p>Phase&nbsp;1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug
 * exists. After the fix (allow {@link Role#ADMIN} to view any order
 * regardless of {@code customerId}, while preserving the existing
 * customer-ownership check for {@link Role#CUSTOMER}), this test must
 * turn green.
 *
 * <p><b>Bug condition C(14):</b> a viewer whose {@link Role role} is
 * {@link Role#ADMIN ADMIN} and whose {@code id} does <i>not</i> match
 * {@code order.customerId}, calling
 * {@link OrderService#getOrderDetail(UUID, User)}.
 *
 * <p><b>Property P(14):</b> for every input satisfying C(14), the call
 * SHALL return an {@link OrderDTO} and SHALL NOT throw a
 * {@link DomainException} carrying {@link ErrorCode#ORDER_ACCESS_DENIED}.
 *
 * <p><b>Buggy production behaviour</b> (see {@link OrderService}):
 * <pre>
 *   if (!order.getCustomerId().equals(viewer.getId())) {
 *       throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
 *   }
 * </pre>
 * The check has no role-based bypass — every ADMIN viewing another
 * customer's order is denied, violating P(14).
 *
 * <p><b>Test design:</b> JUnit&nbsp;5 + Mockito (no Spring context),
 * mirroring {@link OrderServiceBug13ExplorationTest}. The real
 * {@link OrderService} is wired through {@link InjectMocks}; its five
 * collaborators ({@link OrderRepository}, {@link SubOrderRepository},
 * {@link OrderMapper}, {@link SubOrderMapper}, {@link ProductService})
 * are mocked. {@link OrderRepository#findWithDetailById(UUID)} is
 * stubbed to return an order owned by a different customer, and
 * {@link OrderMapper#toDto} is stubbed to return a deterministic DTO so
 * that, after the fix, the ADMIN branch has a happy path to land on.
 * With the bug present the customer-id guard fires before any mapper
 * stub is consulted, so {@link Strictness#LENIENT lenient} stubbing is
 * used to avoid spurious unused-stub failures.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code viewer{role=ADMIN, id=adminId}} +
 * {@code order.customerId == otherCustomerId} (where
 * {@code adminId != otherCustomerId}) →
 * {@code DomainException(ORDER_ACCESS_DENIED)} thrown.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceBug14ExplorationTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private SubOrderMapper subOrderMapper;
    @Mock
    private ProductServiceImpl productService;

    @InjectMocks
    private OrderService orderService;

    /**
     * ADMIN viewer (whose id differs from {@code order.customerId})
     * calls {@code getOrderDetail(orderId)}. After the fix the call
     * must succeed and return an {@link OrderDTO}; today the
     * customer-id guard denies it with {@link ErrorCode#ORDER_ACCESS_DENIED}.
     */
    @Test
    @DisplayName("getOrderDetail must NOT throw ORDER_ACCESS_DENIED for ADMIN viewing another customer's order")
    void getOrderDetail_doesNotDenyForAdmin() {
        // --- Arrange --------------------------------------------------------
        UUID orderId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        UUID otherCustomerId = UUID.randomUUID();
        // C(14) precondition: viewer.id != order.customerId.
        assert !adminId.equals(otherCustomerId);

        User admin = User.builder()
                .name("root")
                .email("admin@example.com")
                .password("hashed")
                .role(Role.ADMIN)
                .build();
        // BaseEntity.id is normally set by Hibernate; set manually so
        // viewer.getId() returns the deterministic adminId we generated.
        ReflectionTestUtils.setField(admin, "id", adminId);

        Order order = Order.builder()
                .customerId(otherCustomerId)
                .status(OrderStatus.IN_PROCESS)
                .totalPrice(new BigDecimal("250.00"))
                .customerName("alice")
                .addressShipping("123 Some Street")
                .phoneNumber("0123456789")
                .build();
        ReflectionTestUtils.setField(order, "id", orderId);

        when(orderRepository.findWithDetailById(orderId))
                .thenReturn(Optional.of(order));

        OrderDTO mappedDto = new OrderDTO(
                orderId,
                OrderStatus.IN_PROCESS,
                new BigDecimal("250.00"),
                "123 Some Street",
                "0123456789",
                "alice",
                List.of(),
                LocalDateTime.now());
        when(orderMapper.toDto(any(Order.class))).thenReturn(mappedDto);

        // --- Act + Assert ---------------------------------------------------
        // Property P(14): ADMIN must NOT be refused by ORDER_ACCESS_DENIED
        // when the viewed order belongs to a different customer. On
        // unfixed code the customer-id guard throws before reaching the
        // stubbed mapper.
        assertThatCode(() -> orderService.getOrderDetail(orderId, admin))
                .as("ADMIN (viewer.id != order.customerId) calling "
                        + "getOrderDetail must not be denied with "
                        + "ORDER_ACCESS_DENIED")
                .doesNotThrowAnyException();
    }
}
