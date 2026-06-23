package com.example.shopnow.product.application.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateShopRequest(
    @NotBlank String name,
    String address,
    String avatarUrl
) {}
