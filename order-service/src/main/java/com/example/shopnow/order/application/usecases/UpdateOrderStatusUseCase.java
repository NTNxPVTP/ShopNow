package com.example.shopnow.order.application.usecases;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.repository.OrderRepository;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public OrderDTO execute(UUID orderId, OrderStatus targetStatus, AuthenticatedUser viewer) {
        Order order = orderRepository.findWithDetailById(orderId)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        order.transitionTo(targetStatus);
        
        // Đơn giản update luôn cho các sub-orders
        if (order.getSubOrders() != null) {
            order.getSubOrders().forEach(subOrder -> subOrder.setStatus(targetStatus));
        }

        Order saved = orderRepository.save(order);
        return orderMapper.toDto(saved);
    }
}
