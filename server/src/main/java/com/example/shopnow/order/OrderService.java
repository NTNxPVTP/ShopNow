package com.example.shopnow.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.OrderDetail;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;
import com.example.shopnow.shared.PageResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    private final OrderMapper mapper;

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

}
