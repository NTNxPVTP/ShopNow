package com.example.shopnow.order.infrastructure.messaging;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        log.info("Received PaymentSuccessEvent for order: {}", event.getOrderId());
        if ("SUCCESS".equals(event.getStatus())) {
            try {
                Order order = orderRepository.findById(event.getOrderId())
                        .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));
                
                order.transitionTo(OrderStatus.PAID);
                orderRepository.save(order);
                
                log.info("Successfully updated order {} to PAID", event.getOrderId());
            } catch (Exception e) {
                log.error("Failed to update order status for order {}: {}", event.getOrderId(), e.getMessage());
                throw e; 
            }
        }
    }
}
