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

        if(!order.getCustomerId().equals(viewer.getId())){
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

        // decrease Quantity and get Products from database
        // Atomic update
        List<ProductInfoForOrder> products = productService.decreaseProducts(itemRequests);
        Map<UUID, ProductInfoForOrder> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoForOrder::id, p -> p));

        // prepare object
        BigDecimal totalPrice = BigDecimal.ZERO;
        Order order = orderMapper.fromRequestToOrder(request);
        List<OrderDetail> orderDetails = new ArrayList<>();

        // check valid request, calculate totalPrice, prepare orderDetails
        for (OrderItemRequest item : request.listItems()) {
            ProductInfoForOrder productInfo = productMap.get(item.productId());
            // add order detail
            orderDetails.add(
                    OrderDetail.builder()
                            .order(order)
                            .productId(productInfo.id())
                            .price(productInfo.price())
                            .productName(productInfo.name())
                            .quantity(item.quantity())
                            .build());

            BigDecimal itemTotal = productInfo.price().multiply(BigDecimal.valueOf(item.quantity()));
            totalPrice = totalPrice.add(itemTotal);
        }

        // finish order entity
        order.setTotalPrice(totalPrice);
        order.setOrderDetails(orderDetails);
        order.setStatus(OrderStatus.IN_PROCESS);
        order.setCustomerId(buyerId);
        // save into database
        order = repository.save(order);

        OrderDTO orderDTO = orderMapper.toDto(order);
        return orderDTO;
    }

}
