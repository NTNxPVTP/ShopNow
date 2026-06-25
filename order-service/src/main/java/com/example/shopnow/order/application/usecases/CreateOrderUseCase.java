package com.example.shopnow.order.application.usecases;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.order.domain.repository.OrderRepository;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderLine;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.api.ProductApi;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that creates an order.
 *
 * <p>It is pure orchestration: it talks to the {@link ProductApi} published
 * port to atomically decrease stock, resolves the returned product info into
 * domain {@link OrderLine}s, then hands all monetary math and sub-order grouping
 * to the {@link Order#create(UUID, String, String, String, List)} domain
 * factory. No totals or grouping are computed here.
 *
 * <p>The single {@link #execute} entry point runs inside one
 * {@code @Transactional} boundary; if any step fails (e.g. the product module
 * raises a {@code DomainException} for out-of-stock, or the domain factory
 * rejects invalid input) the whole transaction rolls back, nothing is saved,
 * and the exception propagates to the caller unchanged.
 */
@Service
@RequiredArgsConstructor
public class CreateOrderUseCase {

    private final OrderRepository orderRepository; // driven port (intra-module)
    private final ProductApi productApi;           // driven port (cross-module)
    private final OrderMapper orderMapper;
    private final com.example.shopnow.order.infrastructure.messaging.OrderEventPublisher orderEventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public OrderDTO execute(CreateOrderRequest request, AuthenticatedUser buyer) {
        List<OrderItemRequest> itemRequests = request.listItems();

        // (a) Atomically decrease stock through the product module's published API.
        List<OrderLineRequest> stockRequests = itemRequests.stream()
                .map(item -> new OrderLineRequest(item.productId(), item.quantity()))
                .toList();
        List<ProductInfoForOrder> products = productApi.getProductsInfo(stockRequests);

        // (b) Resolve each requested item into a domain OrderLine (price, shopId,
        // shopOwnerId and productName resolved from the product info).
        Map<UUID, ProductInfoForOrder> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoForOrder::id, p -> p));

        List<OrderLine> orderLines = itemRequests.stream()
                .map(item -> {
                    ProductInfoForOrder info = productMap.get(item.productId());
                    return new OrderLine(info.id(), info.name(), item.quantity(),
                            info.price(), info.shopId(), info.shopOwnerId());
                })
                .toList();

        // (c) Let the domain build the order: grouping by shopId and all totals
        // are computed inside the aggregate, not here.
        Order order = Order.create(buyer.getId(), request.customerName(),
                request.phoneNumber(), request.addressShipping(), orderLines);

        // (d) Persist through the repository port and (e) map to the response DTO.
        Order saved = orderRepository.save(order);
        
        // (e) Publish event to decrease stock asynchronously
        orderEventPublisher.publishOrderCreatedEvent(
            new com.example.shopnow.order.infrastructure.messaging.OrderCreatedEvent(saved.getId(), stockRequests)
        );
        
        return orderMapper.toDto(saved);
    }
}
