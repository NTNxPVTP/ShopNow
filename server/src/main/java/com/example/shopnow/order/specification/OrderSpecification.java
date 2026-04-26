package com.example.shopnow.order.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.models.OrderStatus;

public class OrderSpecification {
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
