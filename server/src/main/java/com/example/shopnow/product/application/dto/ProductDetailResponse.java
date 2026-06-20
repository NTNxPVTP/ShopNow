package com.example.shopnow.product.application.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import com.example.shopnow.product.domain.models.ProductStatus;

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
