package com.example.shopnow.order.rest.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotEmpty(message = "List items must not be empty")
        @Valid
        List<OrderItemRequest> listItems,
        String addressShipping,
        String phoneNumber,
        String customerName) {
}

