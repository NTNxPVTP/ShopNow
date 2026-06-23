package com.example.shopnow.order.domain.repository;

import java.util.UUID;
import com.example.shopnow.order.domain.models.OrderStatus;

/**
 * Pure query object carrying the dynamic filter criteria used when listing or
 * paginating orders. It replaces the leakage of Spring Data {@code Specification}
 * into the {@code application} and {@code domain} layers: the application layer
 * builds an {@link OrderQuery} and the persistence adapter translates it into a
 * {@code Specification} internally.
 *
 * <p>This is a framework-neutral domain value type — it intentionally carries no
 * Spring Data ({@code org.springframework.data.*}) nor JPA
 * ({@code jakarta.persistence.*}) types.
 *
 * @param customerId filter by owning customer, or {@code null} to ignore
 * @param status     filter by order status, or {@code null} to ignore
 * @param shopId     filter by shop, or {@code null} to ignore
 */
public record OrderQuery(UUID customerId, OrderStatus status, UUID shopId) {
}
