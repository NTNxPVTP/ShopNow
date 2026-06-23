package com.example.shopnow.order.domain.models;

import java.util.Set;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    DELIVERING,
    DELIVERED;

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return allowedTransitions().contains(target);
    }

    private Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING_PAYMENT -> Set.of(PAID);
            case PAID -> Set.of(DELIVERING);
            case DELIVERING -> Set.of(DELIVERED);
            case DELIVERED -> Set.of();
        };
    }
}
