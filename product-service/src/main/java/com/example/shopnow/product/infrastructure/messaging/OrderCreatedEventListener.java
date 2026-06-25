package com.example.shopnow.product.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.example.shopnow.product.application.usecases.DecreaseStockUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventListener {

    private final DecreaseStockUseCase decreaseStockUseCase;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());
        try {
            decreaseStockUseCase.execute(event.getItems());
            log.info("Successfully decreased stock for order: {}", event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to decrease stock for order {}: {}", event.getOrderId(), e.getMessage());
            // In a real system, you might want to send a compensating event back to order-service to cancel the order
            throw e; 
        }
    }
}
