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
    public OrderDTO getOrderDetail(UUID id) {
        Order order = repository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
        return orderMapper.toDto(order);
    }

    // has not check permission, has not have specification
    public PageResponse<OrderDTO> getOrders(Pageable pageable) {
        Page<Order> orders = repository.findWithPageReponseBy(pageable);
        return orderMapper.toPageResponse(orders);
    }

    // TODO: write UnitTest
    // has not handle race condition
    // has not set the payment method
    // has not insert batching
    // has not mapping to person
    // has not sent event to reduce quantity from Product
    // has not valid
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request, User buyer) {
        UUID buyerId = buyer.getId();

        // get Products from request
        List<UUID> productIds = request.listItems().stream()
                .map(OrderItemRequest::productId) // Method Reference
                .toList();

        // get Products from database
        List<ProductInfoForOrder> products = productService.getProductsForOrder(productIds);

        Map<UUID, ProductInfoForOrder> productMap = products.stream()
                .collect(Collectors.toMap(ProductInfoForOrder::id, p -> p));

        // prepare object
        BigDecimal totalPrice = BigDecimal.ZERO;
        Order order = orderMapper.fromRequestToOrder(request);
        List<OrderDetail> orderDetails = new ArrayList<>();

        // check valid request, calculate totalPrice, prepare orderDetails
        for (OrderItemRequest item : request.listItems()) {
            ProductInfoForOrder productDto = productMap.get(item.productId());

            // Check exist
            System.out.println("Check exist product here!");
            if (productDto == null) {
                throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
            }

            // Check quantity
            if (productDto.quantity() < item.quantity()) {
                throw new DomainException(ErrorCode.INSUFFICIENT_STOCK);
            }

            // add order detail
            orderDetails.add(
                    OrderDetail.builder()
                            .order(order)
                            .productId(productDto.id())
                            .price(productDto.price())
                            .productName(productDto.name())
                            .quantity(item.quantity())
                            .build());

            BigDecimal itemTotal = productDto.price().multiply(BigDecimal.valueOf(item.quantity()));
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
