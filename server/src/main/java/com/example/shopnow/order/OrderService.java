package com.example.shopnow.order;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.models.*;
import com.example.shopnow.order.specification.OrderSpecification;
import com.example.shopnow.order.specification.SubOrderSpecification;
import com.example.shopnow.order.rest.dto.*;
import com.example.shopnow.product.ProductService;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.Role;
import com.example.shopnow.user.models.User;
import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final SubOrderRepository subOrderRepository;
    private final OrderMapper orderMapper;
    private final SubOrderMapper subOrderMapper;
    private final ProductService productService;

    // has not check permission
    public OrderDTO getOrderDetail(UUID id, User viewer) {

        Order order = orderRepository.findWithDetailById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getCustomerId().equals(viewer.getId())) {
            System.out.println(order.getCustomerId());
            System.out.println(viewer.getId());
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }
        return orderMapper.toDto(order);
    }

    public PageResponse<OrderSummaryDTO> getOrders(Pageable pageable, User customer, OrderStatus status, UUID shopId) {
        if (customer == null || customer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        UUID customerId = customer.getId();
        Specification<Order> specification = Specification
                .where(OrderSpecification.hasStatus(status)
                .and(OrderSpecification.hasShopId(shopId))
                .and(OrderSpecification.hasCustomerId(customerId))
                );

        Page<Order> orders = orderRepository.findAll(specification, pageable);
        return orderMapper.toSummaryPageResponse(orders);
    }

    public SubOrderDTO getSubOrderDetail(UUID id, User viewer) {
        if (viewer == null || viewer.getRole() == null || viewer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        SubOrder subOrder;

        if (!viewer.getRole().equals(Role.SELLER)) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        subOrder = subOrderRepository.findWithDetailByIdAndShopOwnerId(id, viewer.getId())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        return subOrderMapper.toDto(subOrder);
    }

    public PageResponse<SubOrderSummaryDTO> getSubOrders(Pageable pageable, User viewer, OrderStatus status,
            UUID shopId) {
        if (viewer == null || viewer.getRole() == null || viewer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Page<SubOrder> subOrders;

        if (!viewer.getRole().equals(Role.SELLER)) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        Specification<SubOrder> specification = Specification
                .where(SubOrderSpecification.hasStatus(status))
                .and(SubOrderSpecification.hasShopId(shopId))
                .and(SubOrderSpecification.hasShopOwnerId(viewer.getId()))
                ;

        subOrders = subOrderRepository.findAll(specification, pageable);
        
        return subOrderMapper.toSummaryPageResponse(subOrders);
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
        Set<SubOrder> subOrders = new HashSet<>();

        for (Map.Entry<UUID, List<OrderItemRequest>> entry : itemsByShop.entrySet()) {
            UUID shopId = entry.getKey();
            List<OrderItemRequest> shopItems = entry.getValue();
            UUID shopOwnerId = productMap.get(shopItems.get(0).productId()).shopOwnerId();
            System.out.println("Shop owwner here: ");
            System.out.println(shopOwnerId);

            BigDecimal subOrderTotal = BigDecimal.ZERO;
            Set<OrderDetail> details = new HashSet<>();

            SubOrder subOrder = SubOrder.builder()
                    .order(parentOrder)
                    .shopId(shopId)
                    .shopOwnerId(shopOwnerId)
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

        parentOrder = orderRepository.save(parentOrder);
        System.out.println("parentOrder herre: ");

        System.out.println(parentOrder);
        return orderMapper.toDto(parentOrder);
    }

}
