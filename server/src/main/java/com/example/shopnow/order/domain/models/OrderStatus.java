package com.example.shopnow.order.domain.models;

import java.util.Set;

public enum OrderStatus {
    IN_PROCESS,
    DELIVERING,
    PAID;

    /**
     * Defines the allowed forward transitions in the order lifecycle:
     * {@code IN_PROCESS -> DELIVERING -> PAID}. No transition is allowed out of
     * {@code PAID} (terminal state), and a status cannot transition to itself.
     *
     * @param target the status to transition to
     * @return {@code true} if moving from this status to {@code target} is allowed
     */
    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return allowedTransitions().contains(target);
    }

    private Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case IN_PROCESS -> Set.of(DELIVERING);
            case DELIVERING -> Set.of(PAID);
            case PAID -> Set.of();
        };
    }
}
