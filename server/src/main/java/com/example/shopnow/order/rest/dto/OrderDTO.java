package com.example.shopnow.order.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.example.shopnow.order.models.OrderStatus;

public record OrderDTO(
    UUID id,
    OrderStatus status,
    BigDecimal totalPrice,
    String addressShipping,
    String phoneNumber,
    String customerName,
    List<SubOrderDTO> subOrders,
    LocalDateTime createdAt
) {  
}
