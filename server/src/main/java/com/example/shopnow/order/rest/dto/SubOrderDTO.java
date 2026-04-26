package com.example.shopnow.order.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.shopnow.order.models.OrderStatus;

public record SubOrderDTO(
    UUID id,
    UUID shopId,
    OrderStatus status,
    BigDecimal totalPrice,
    List<OrderDetailDTO> orderDetails,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
}
