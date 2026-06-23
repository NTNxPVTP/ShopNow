package com.example.shopnow.product.api.dto;

import java.util.UUID;

public record OrderLineRequest(UUID productId, Integer quantity) {}
