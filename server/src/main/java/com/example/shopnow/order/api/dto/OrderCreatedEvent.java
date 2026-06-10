package com.example.shopnow.order.api.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderCreatedEvent(UUID orderId, UUID customerId, Instant createdAt) {}
