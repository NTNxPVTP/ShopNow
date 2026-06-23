package com.example.shopnow.cart.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductInfo(
    UUID id,
    String name,
    BigDecimal price,
    Integer quantity
) {}
