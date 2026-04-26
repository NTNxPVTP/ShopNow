package com.example.shopnow.order.rest.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.shopnow.order.models.OrderStatus;

public record SubOrderSummaryDTO(
    UUID id,
    UUID shopId,
    OrderStatus status,
    Integer totalPrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
}
