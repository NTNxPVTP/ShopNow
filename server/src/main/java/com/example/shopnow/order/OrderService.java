package com.example.shopnow.order;

import java.util.List;
import java.util.UUID;

import org.antlr.v4.runtime.atn.SemanticContext.OR;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDetail;
import com.example.shopnow.product.ProductService;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.shared.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final ProductService productService;

    //has not check permission
    public OrderDetail getOrderDetail(UUID id){
        Order order = repository.findById(id)
                    .orElseThrow(()->new DomainException(ErrorCode.ORDER_NOT_FOUND));
        return mapper.toDto(order);
    }

    //has not check permission, has not have specification
    public PageResponse<OrderDetail> getOrders(Pageable pageable){
        Page<Order> orders = repository.findWithPageReponseBy(pageable);
        return mapper.toPageResponse(orders);
    }

   
    //TODO: write UnitTest
    //has not handle race condition
    //has not set the payment method
    // @Transactional
    // public OrderDetail createOrder(CreateOrderRequest request){
    //     List<UUID> productIds = request.listItems().stream()
    //                                 .map(item -> item.productId())
    //                                 .toList();
    //     List<Product> products = productService.findAllByIdIn(productIds);
        

    // }

    
}
