package com.example.shopnow.payment.application.dto;

import java.math.BigDecimal;
import java.util.UUID;
import com.example.shopnow.payment.domain.models.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Payment method is required")
    private PaymentMethod method;
}
