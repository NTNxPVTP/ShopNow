package com.example.shopnow.order.rest.dto;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotEmpty(message = "List items must not be empty")
        @Valid
        List<OrderItemRequest> listItems,
        @NotBlank(message = "Address shipping must not be blank")
        String addressShipping,
        @NotBlank(message = "Phone number must not be blank")
        String phoneNumber,
        @NotBlank(message = "Customer name must not be blank")
        String customerName) {
}

