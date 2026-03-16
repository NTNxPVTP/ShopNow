package com.example.shopnow.product.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.shopnow.product.models.ProductStatus;

import jakarta.validation.constraints.NotNull;

public record ProductDetailResponse(
    @NotNull(message =  "Id must not be null")
    UUID id,
    @NotNull(message = "Product's name must not be null")
    String name,
    @NotNull(message = "Picture must not be null")
    String picture_url,
    @NotNull(message = "Quantity must not be null")
    Integer quantity,
    @NotNull(message = "Price must not be null")
    BigDecimal price,
    @NotNull(message = "Status must not be null")
    ProductStatus status

) {}
