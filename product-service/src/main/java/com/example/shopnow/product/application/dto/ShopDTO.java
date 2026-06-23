package com.example.shopnow.product.application.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShopDTO(
    UUID id,
    String name,
    String address,
    String avatarUrl,
    Boolean isActive,
    UUID ownerId,
    LocalDateTime createdAt
) {}
