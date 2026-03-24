package com.example.shopnow.order.rest.dto;

import java.math.BigDecimal;

public record OrderDetailDTO(
    String productName,
    BigDecimal price,
    Integer quantity
) {
    
}
