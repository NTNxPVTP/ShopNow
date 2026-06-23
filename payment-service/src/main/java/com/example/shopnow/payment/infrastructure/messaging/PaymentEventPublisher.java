package com.example.shopnow.payment.infrastructure.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishPaymentSuccessEvent(PaymentSuccessEvent event) {
        log.info("Publishing PaymentSuccessEvent for order: {}", event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, "payment.success", event);
    }
}
