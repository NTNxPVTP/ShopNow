package com.example.shopnow.order.rest.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.shopnow.order.models.OrderStatus;

public record SubOrderDTO(
    UUID id,
    UUID shopId,
    OrderStatus status,
    Integer totalPrice,
    List<OrderDetailDTO> orderDetails,
    LocalDateTime createdAt,
    LocalDateTime udpateddAt
) {
    
}
