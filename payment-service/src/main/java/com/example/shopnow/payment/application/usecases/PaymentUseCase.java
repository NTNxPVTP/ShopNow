package com.example.shopnow.payment.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.payment.api.OrderApiClient;
import com.example.shopnow.payment.api.OrderInfo;
import com.example.shopnow.payment.application.dto.PaymentRequest;
import com.example.shopnow.payment.application.dto.PaymentResponse;
import com.example.shopnow.payment.domain.models.Payment;
import com.example.shopnow.payment.domain.models.PaymentStatus;
import com.example.shopnow.payment.domain.repository.PaymentRepository;
import com.example.shopnow.payment.infrastructure.messaging.PaymentEventPublisher;
import com.example.shopnow.payment.infrastructure.messaging.PaymentSuccessEvent;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final OrderApiClient orderApiClient;
    private final PaymentEventPublisher paymentEventPublisher;

    @Transactional
    public PaymentResponse processPayment(AuthenticatedUser user, PaymentRequest request) {
        OrderInfo orderInfo;
        try {
            orderInfo = orderApiClient.getOrderInfo(request.getOrderId());
        } catch (Exception e) {
            throw new DomainException(ErrorCode.ORDER_NOT_FOUND);
        }

        if (!orderInfo.customerId().equals(user.getId())) {
            throw new DomainException(ErrorCode.ORDER_NOT_OWNED);
        }

        if (orderInfo.totalPrice().compareTo(request.getAmount()) != 0) {
            throw new DomainException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        Payment payment = new Payment(
                request.getOrderId(),
                request.getAmount(),
                PaymentStatus.SUCCESS,
                request.getMethod(),
                java.time.LocalDateTime.now()
        );
        paymentRepository.save(payment);

        // Publish event to RabbitMQ instead of synchronous REST call
        paymentEventPublisher.publishPaymentSuccessEvent(
                PaymentSuccessEvent.builder()
                        .orderId(payment.getOrderId())
                        .amount(payment.getAmount())
                        .status("SUCCESS")
                        .build()
        );

        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getCreatedAt()
        );
    }
}
