package com.example.shopnow.product.rest.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import com.example.shopnow.product.models.ProductStatus;

public record ProductDetailResponse(
    UUID id,
    String name,
    String pictureUrl,
    Integer quantity,
    BigDecimal price,
    ProductStatus status,
    UUID shopId,
    Set<UUID> categoryIds

) {}
