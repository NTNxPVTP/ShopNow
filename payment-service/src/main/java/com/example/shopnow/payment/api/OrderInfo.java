package com.example.shopnow.payment.api;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderInfo(
    UUID id,
    UUID customerId,
    BigDecimal totalPrice,
    String status
) {}
