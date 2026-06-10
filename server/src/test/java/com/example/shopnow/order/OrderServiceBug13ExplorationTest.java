package com.example.shopnow.order;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.product.ProductServiceImpl;
import com.example.shopnow.shared.PageInfo;
import com.example.shopnow.shared.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
 * Exploration test for BUG-13 ({@link OrderService#getSubOrderDetail} and
 * {@link OrderService#getSubOrders} unconditionally reject every viewer
 * whose role is not {@link Role#SELLER} via {@code if
 * (!viewer.getRole().equals(Role.SELLER)) throw ORDER_ACCESS_DENIED}).
 *
 * <p><b>Validates: Requirements 2.13</b> (Property 13 in design.md).
 *
 * <p>Phase&nbsp;1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug
 * exists. After the fix (replace the SELLER-only guard with role-aware
 * routing — CUSTOMER may view sub-orders attached to their own parent
 * order, ADMIN may view every sub-order, SELLER keeps the existing
 * scoped-to-shop view), this test must turn green.
 *
 * <p><b>Bug condition C(13):</b> a viewer whose
 * {@link Role role} is one of {@link Role#CUSTOMER CUSTOMER} (when the
 * caller owns the parent order) or {@link Role#ADMIN ADMIN}, calling
 * either {@link OrderService#getSubOrderDetail} (case&nbsp;a) or
 * {@link OrderService#getSubOrders} (case&nbsp;b).
 *
 * <p><b>Property P(13):</b> for every input satisfying C(13), the call
 * SHALL NOT throw {@link DomainException} carrying
 * {@link ErrorCode#ORDER_ACCESS_DENIED}; legitimate viewers must be
 * routed to the proper read path. The two scenarios asserted here are:
 * <ol>
 *   <li><b>Case&nbsp;(a):</b> viewer with {@code role = CUSTOMER} and
 *       {@code viewer.id == subOrder.parentOrder.customerId} calling
 *       {@code getSubOrderDetail(id)}.</li>
 *   <li><b>Case&nbsp;(b):</b> viewer with {@code role = ADMIN} calling
 *       {@code getSubOrders(pageable, admin, status, shopId)}.</li>
 * </ol>
 *
 * <p><b>Buggy production behaviour</b> (see {@link OrderService}):
 * <pre>
 *   if (!viewer.getRole().equals(Role.SELLER)) {
 *       throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
 *   }
 * </pre>
 * The check is identical in both methods — every CUSTOMER and every
 * ADMIN is denied before any ownership lookup happens, so case&nbsp;(a)
 * and case&nbsp;(b) both fail P(13).
 *
 * <p><b>Test design:</b> JUnit&nbsp;5 + Mockito (no Spring context).
 * The real {@link OrderService} is wired through {@link InjectMocks};
 * its five collaborators ({@link OrderRepository},
 * {@link SubOrderRepository}, {@link OrderMapper}, {@link SubOrderMapper},
 * {@link ProductService}) are mocked. Repository / mapper return values
 * are pre-stubbed so that, after the fix, the role-allowed branches
 * have a happy path to land on; with the bug present, the SELLER guard
 * fires before any stub is consulted, so {@link Strictness#LENIENT
 * lenient} stubbing is used to avoid spurious unused-stub failures.
 * Both scenarios assert {@code doesNotThrowAnyException()} via AssertJ
 * — the SELLER guard surfaces as a {@link DomainException} with code
 * {@link ErrorCode#ORDER_ACCESS_DENIED}, which violates P(13).
 *
 * <p><b>Expected counterexamples on unfixed code:</b>
 * <ul>
 *   <li>Case&nbsp;(a): {@code viewer{role=CUSTOMER, id=customerId}} +
 *       {@code subOrder.order.customerId == customerId} →
 *       {@code DomainException(ORDER_ACCESS_DENIED)} thrown.</li>
 *   <li>Case&nbsp;(b): {@code viewer{role=ADMIN}} →
 *       {@code DomainException(ORDER_ACCESS_DENIED)} thrown.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceBug13ExplorationTest {

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
     * Case (a) — CUSTOMER who owns the parent order calls
     * {@code getSubOrderDetail(subOrderId)}. After the fix the call
     * must succeed; today the SELLER-only guard denies it.
     */
    @Test
    @DisplayName("getSubOrderDetail must NOT throw ORDER_ACCESS_DENIED for CUSTOMER who owns the parent order")
    void getSubOrderDetail_doesNotDenyForOwningCustomer() {
        // --- Arrange --------------------------------------------------------
        UUID customerId = UUID.randomUUID();
        UUID subOrderId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID shopOwnerId = UUID.randomUUID();

        User viewer = User.builder()
                .name("alice")
                .email("alice@example.com")
                .password("hashed")
                .role(Role.CUSTOMER)
                .build();
        // BaseEntity.id is normally set by Hibernate; set manually so
        // viewer.getId() == subOrder.order.customerId (the ownership
        // invariant the fix is required to honour).
        ReflectionTestUtils.setField(viewer, "id", customerId);

        Order parentOrder = Order.builder()
                .customerId(customerId)
                .status(OrderStatus.IN_PROCESS)
                .totalPrice(new BigDecimal("100.00"))
                .customerName("alice")
                .build();
        ReflectionTestUtils.setField(parentOrder, "id", UUID.randomUUID());

        SubOrder subOrder = SubOrder.builder()
                .order(parentOrder)
                .shopId(shopId)
                .shopOwnerId(shopOwnerId)
                .status(OrderStatus.IN_PROCESS)
                .totalPrice(new BigDecimal("100.00"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(subOrder, "id", subOrderId);

        // Stub both candidate finders so the test stays robust to which
        // repository method the fix decides to call for the CUSTOMER
        // branch (id-only lookup is the most likely choice; shop-scoped
        // lookup is what the SELLER branch already uses).
        when(subOrderRepository.findWithDetailById(subOrderId))
                .thenReturn(Optional.of(subOrder));
        when(subOrderRepository.findWithDetailByIdAndShopOwnerId(any(UUID.class), any(UUID.class)))
                .thenReturn(Optional.of(subOrder));

        SubOrderDTO mappedDto = new SubOrderDTO(
                subOrderId,
                shopId,
                OrderStatus.IN_PROCESS,
                new BigDecimal("100.00"),
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
        when(subOrderMapper.toDto(any(SubOrder.class))).thenReturn(mappedDto);

        // --- Act + Assert ---------------------------------------------------
        // Property P(13).a: CUSTOMER who owns the parent order must NOT be
        // refused by ORDER_ACCESS_DENIED. On unfixed code the SELLER guard
        // throws before reaching any of the stubbed collaborators.
        assertThatCode(() -> orderService.getSubOrderDetail(subOrderId, viewer))
                .as("CUSTOMER (viewer.id == subOrder.parentOrder.customerId) "
                        + "calling getSubOrderDetail must not be denied with "
                        + "ORDER_ACCESS_DENIED")
                .doesNotThrowAnyException();
    }

    /**
     * Case (b) — ADMIN calls {@code getSubOrders()}. After the fix the
     * call must succeed and return every sub-order; today the
     * SELLER-only guard denies it.
     */
    @Test
    @DisplayName("getSubOrders must NOT throw ORDER_ACCESS_DENIED for ADMIN")
    @SuppressWarnings("unchecked")
    void getSubOrders_doesNotDenyForAdmin() {
        // --- Arrange --------------------------------------------------------
        UUID adminId = UUID.randomUUID();
        User admin = User.builder()
                .name("root")
                .email("admin@example.com")
                .password("hashed")
                .role(Role.ADMIN)
                .build();
        ReflectionTestUtils.setField(admin, "id", adminId);

        Pageable pageable = PageRequest.of(0, 20);

        Page<SubOrder> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(subOrderRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        PageResponse<SubOrderSummaryDTO> mappedPage = new PageResponse<>(
                List.of(),
                PageInfo.builder()
                        .pageNumber(0)
                        .pageSize(20)
                        .totalPages(0)
                        .isLast(true)
                        .totalElements(0L)
                        .build());
        when(subOrderMapper.toSummaryPageResponse(any())).thenReturn(mappedPage);

        // --- Act + Assert ---------------------------------------------------
        // Property P(13).b: ADMIN must NOT be refused by ORDER_ACCESS_DENIED.
        // On unfixed code the SELLER guard throws before reaching the
        // stubbed repository / mapper.
        assertThatCode(() -> orderService.getSubOrders(pageable, admin, null, null))
                .as("ADMIN calling getSubOrders must not be denied with "
                        + "ORDER_ACCESS_DENIED")
                .doesNotThrowAnyException();
    }
}
