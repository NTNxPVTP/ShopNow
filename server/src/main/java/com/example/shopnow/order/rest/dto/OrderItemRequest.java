package com.example.shopnow.order.rest.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Product ID must not be null") UUID productId,
        @Min(value = 1, message = "Quantity must be greater than 0") Integer quantity) {
}
