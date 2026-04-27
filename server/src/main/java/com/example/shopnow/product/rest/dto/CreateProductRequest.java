package com.example.shopnow.product.rest.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotNull(message = "Product's name must not be null") String name,
        String pictureUrl,
        @NotNull(message = "Quantity must not be null") Integer quantity,
        @NotNull(message = "Price must not be null") BigDecimal price,
        @NotNull(message = "Shop id must not be null") UUID shopId,
        Set<UUID> categoryIds) {
}
