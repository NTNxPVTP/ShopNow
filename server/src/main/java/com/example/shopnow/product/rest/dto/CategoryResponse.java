package com.example.shopnow.product.rest.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name) {
}
