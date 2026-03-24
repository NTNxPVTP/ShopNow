package com.example.shopnow.order.rest.dto;

import java.util.UUID;

import com.example.shopnow.order.models.OrderStatus;

    
public record OrderDetailResponse(
    OrderStatus status,
    Integer totalPrice,
    String addressShipping,
    String phoneNumber,
    String customerName
){}
