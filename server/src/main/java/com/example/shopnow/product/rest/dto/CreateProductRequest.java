package com.example.shopnow.product.rest.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotNull(message = "Product's name must not be null") String name,
        String pictureUrl,
        @NotNull(message = "Quantity must not be null") Integer quantity,
        @NotNull(message = "Price must not be null") BigDecimal price) {
}
