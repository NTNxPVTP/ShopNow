package com.example.shopnow.order.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;

public class OrderSpecification {

    /**
     * Builds a composite {@link Specification} from a pure {@link OrderQuery}
     * by composing the {@code customerId}, {@code status} and {@code shopId}
     * predicates. A {@code null} query is treated as match-all.
     *
     * <p>This keeps Spring Data {@code Specification} confined to the
     * {@code infrastructure/persistence} package: the application layer passes
     * an {@link OrderQuery} and the persistence adapter translates it here.
     */
    public static Specification<Order> from(OrderQuery query) {
        if (query == null) {
            return Specification.allOf();
        }
        return Specification.allOf(
                hasCustomerId(query.customerId()),
                hasStatus(query.status()),
                hasShopId(query.shopId()));
    }

    public static Specification<Order> hasOrderId(UUID orderId) {
        return (root, query, cb) -> {
            if (orderId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("id"), orderId);
        };
    }

    public static Specification<Order> hasShopId(UUID shopId) {
        return (root, query, cb) -> {
            if (shopId == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            return cb.equal(root.join("subOrders").get("shopId"), shopId);
        };
    }

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Order> hasCustomerId(UUID customerId) {
        return (root, query, cb) -> {
            if (customerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("customerId"), customerId);
        };
    }
}
