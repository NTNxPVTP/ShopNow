package com.example.shopnow.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.models.OrderDetail;
import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.models.SubOrder;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderItemRequest;
import com.example.shopnow.product.ProductService;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.User;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper orderMapper;
    private final ProductService productService;

    // has not check permission
    public OrderDTO getOrderDetail(UUID id, User viewer) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomerId().equals(viewer.getId())) {
            System.out.println(order.getCustomerId());
            System.out.println(viewer.getId());
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return orderMapper.toDto(order);
    }

    // has not check permission, has not have specification
    public PageResponse<OrderDTO> getOrders(Pageable pageable, User customer) {
        UUID customerId = customer.getId();
        Page<Order> orders = repository.findWithPageReponseByCustomerId(pageable, customerId);
        return orderMapper.toPageResponse(orders);
    }

    // TODO: write UnitTest
    // has not set the payment method
    // has not insert batching
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, User buyer) {
        UUID buyerId = buyer.getId();
        List<OrderItemRequest> itemRequests = request.listItems();

        // decrease product and get product (Atomic Update)
        List<ProductInfoForOrder> products = productService.decreaseProducts(itemRequests);

        // prepare map product
        Map<UUID, ProductInfoForOrder> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoForOrder::id, p -> p));

        Order parentOrder = orderMapper.fromRequestToOrder(request);
        parentOrder.setCustomerId(buyerId);
        parentOrder.setStatus(OrderStatus.IN_PROCESS);

        // Grouping item request by shopId
        Map<UUID, List<OrderItemRequest>> itemsByShop = itemRequests.stream()
                .collect(Collectors.groupingBy(item -> productMap.get(item.productId()).shopId()));

        BigDecimal grandTotal = BigDecimal.ZERO;
        List<SubOrder> subOrders = new ArrayList<>();

        for (Map.Entry<UUID, List<OrderItemRequest>> entry : itemsByShop.entrySet()) {
            UUID shopId = entry.getKey();
            List<OrderItemRequest> shopItems = entry.getValue();

            BigDecimal subOrderTotal = BigDecimal.ZERO;
            List<OrderDetail> details = new ArrayList<>();

            
            SubOrder subOrder = SubOrder.builder()
                    .order(parentOrder)
                    .shopId(shopId)
                    .status(OrderStatus.IN_PROCESS)
                    .build();

            
            for (OrderItemRequest item : shopItems) {
                ProductInfoForOrder pInfo = productMap.get(item.productId());
                BigDecimal itemPrice = pInfo.price();
                BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(item.quantity()));

                subOrderTotal = subOrderTotal.add(itemTotal);

                details.add(OrderDetail.builder()
                        .subOrder(subOrder)
                        .productId(pInfo.id())
                        .productName(pInfo.name())
                        .price(itemPrice)
                        .quantity(item.quantity())
                        .build());
            }

            subOrder.setTotalPrice(subOrderTotal);
            subOrder.setOrderDetails(details);

            // add SubOrder into Parent Order
            subOrders.add(subOrder);
            grandTotal = grandTotal.add(subOrderTotal);
        }

        // update total price
        parentOrder.setTotalPrice(grandTotal);
        parentOrder.setSubOrders(subOrders);

        parentOrder = repository.save(parentOrder);
        return orderMapper.toDto(parentOrder);
    }

}
