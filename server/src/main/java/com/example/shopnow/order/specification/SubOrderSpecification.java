package com.example.shopnow.order.specification;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.models.SubOrder;

public class SubOrderSpecification {
    public static Specification<SubOrder> hasShopId(UUID shopId) {
        return (root, query, cb) -> {
            if (shopId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("shopId"), shopId);
        };
    }

    public static Specification<SubOrder> hasStatus(OrderStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<SubOrder> hasShopOwnerId(UUID shopOwnerId) {
        return (root, query, cb) -> {
            if (shopOwnerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("shopOwnerId"), shopOwnerId);
        };
    }
}
