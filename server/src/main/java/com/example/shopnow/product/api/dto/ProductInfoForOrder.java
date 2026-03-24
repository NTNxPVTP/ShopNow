package com.example.shopnow.product.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfoForOrder(
    UUID id,
    BigDecimal price,
    String name,
    Integer quantity
) {
}
