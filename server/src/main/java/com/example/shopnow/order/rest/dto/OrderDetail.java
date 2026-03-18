package com.example.shopnow.order.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopnow.order.models.OrderStatus;

public record OrderDetail(
    UUID id,
    OrderStatus status,
    BigDecimal totalPrice,
    String addressShipping,
    String phoneNumber,
    String customerName,
    LocalDateTime createdAt
) {  
}
