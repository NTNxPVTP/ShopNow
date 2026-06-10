package com.example.shopnow.order;

import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.api.ProductApi;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.user.api.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit test for the {@code OrderItemRequest -> OrderLineRequest} mapping performed inside
 * {@link OrderService#createOrder(CreateOrderRequest, AuthenticatedUser)}.
 *
 * <p><b>Validates: Requirements 4.2</b>
 *
 * <p>The production code maps each inbound {@link OrderItemRequest} to a
 * {@link OrderLineRequest} (Product module's Published API DTO) before calling
 * {@link ProductApi#decreaseProducts(List)}:
 * <pre>
 *   List&lt;OrderLineRequest&gt; orderLines = itemRequests.stream()
 *       .map(item -&gt; new OrderLineRequest(item.productId(), item.quantity()))
 *       .toList();
 * </pre>
 * This test drives the real {@code createOrder} path with mocked collaborators and
 * captures the {@code List<OrderLineRequest>} actually handed to {@code decreaseProducts}
 * via an {@link ArgumentCaptor}, then asserts the mapping is a loss-free, order-preserving
 * 1-1 transformation of the inbound items (same size, same {@code productId}/{@code quantity}
 * in the same order).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceMappingTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private SubOrderRepository subOrderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private SubOrderMapper subOrderMapper;
    @Mock
    private ProductApi productApi;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<List<OrderLineRequest>> orderLinesCaptor;

    @Test
    @DisplayName("createOrder maps OrderItemRequest -> OrderLineRequest 1-1, preserving productId/quantity and order")
    void createOrder_mapsItemsToOrderLines_oneToOne() {
        // --- Arrange --------------------------------------------------------
        UUID buyerId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        UUID shopOwnerId = UUID.randomUUID();

        UUID productId1 = UUID.randomUUID();
        UUID productId2 = UUID.randomUUID();
        UUID productId3 = UUID.randomUUID();

        // Distinct productId/quantity to detect any loss, swap or reordering.
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(productId1, 2),
                new OrderItemRequest(productId2, 5),
                new OrderItemRequest(productId3, 1));

        CreateOrderRequest request = new CreateOrderRequest(
                items,
                "123 Some Street",
                "0123456789",
                "alice");

        AuthenticatedUser buyer = org.mockito.Mockito.mock(AuthenticatedUser.class);
        when(buyer.getId()).thenReturn(buyerId);

        // decreaseProducts must return product info for each input product so the
        // downstream productMap.get(...) calls in createOrder do not NPE.
        List<ProductInfoForOrder> products = List.of(
                new ProductInfoForOrder(productId1, new BigDecimal("10.00"), "P1", 100, shopId, shopOwnerId),
                new ProductInfoForOrder(productId2, new BigDecimal("20.00"), "P2", 100, shopId, shopOwnerId),
                new ProductInfoForOrder(productId3, new BigDecimal("30.00"), "P3", 100, shopId, shopOwnerId));
        when(productApi.decreaseProducts(anyList())).thenReturn(products);

        when(orderMapper.fromRequestToOrder(request)).thenReturn(new Order());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        OrderDTO dummyDto = new OrderDTO(
                UUID.randomUUID(), null, BigDecimal.ZERO,
                "123 Some Street", "0123456789", "alice",
                List.of(), LocalDateTime.now());
        when(orderMapper.toDto(any(Order.class))).thenReturn(dummyDto);

        // --- Act ------------------------------------------------------------
        orderService.createOrder(request, buyer);

        // --- Assert ---------------------------------------------------------
        org.mockito.Mockito.verify(productApi).decreaseProducts(orderLinesCaptor.capture());
        List<OrderLineRequest> captured = orderLinesCaptor.getValue();

        // Same size: no item dropped or duplicated.
        assertThat(captured).hasSameSizeAs(items);

        // 1-1, order-preserving, loss-free mapping of productId + quantity.
        for (int i = 0; i < items.size(); i++) {
            OrderItemRequest expected = items.get(i);
            OrderLineRequest actual = captured.get(i);
            assertThat(actual.productId())
                    .as("productId preserved at index %d", i)
                    .isEqualTo(expected.productId());
            assertThat(actual.quantity())
                    .as("quantity preserved at index %d", i)
                    .isEqualTo(expected.quantity());
        }
    }
}
