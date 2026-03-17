package com.example.shopnow.product.rest.dto;

import java.math.BigDecimal;

public record UpdateProductRequest(
        String name,
        String pictureUrl,
        Integer quantity,
        BigDecimal price) {
}
