package com.example.shopnow.product;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;

public class ProductSpecification {
    public static Specification<Product> isNotDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<Product> hasProductId(UUID productId) {
        return (root, query, cb) -> {
            if (productId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("id"), productId);
        };
    }

    public static Specification<Product> hasShopId(UUID shopId) {
        return (root, query, cb) -> {
            if (shopId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.join("shop").get("id"), shopId);
        };
    }

    public static Specification<Product> hasShopOwnerId(UUID shopOwnerId) {
        return (root, query, cb) -> {
            if (shopOwnerId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.join("shop").get("ownerId"), shopOwnerId);
        };
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Product> hasCategoryId(UUID categoryId) {
        return (root, query, cb) -> {
            if (categoryId == null) {
                return cb.conjunction();
            }
            query.distinct(true);
            return cb.equal(root.join("categories").get("id"), categoryId);
        };
    }

    public static Specification<Product> hasNameLike(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("name")), "%" + keyword.trim().toLowerCase() + "%");
        };
    }

    public static Specification<Product> hasPriceGreaterThanOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("price"), minPrice);
        };
    }

    public static Specification<Product> hasPriceLessThanOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) {
                return cb.conjunction();
            }
            return cb.lessThanOrEqualTo(root.get("price"), maxPrice);
        };
    }

    public static Specification<Product> isInStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("quantity"), 0);
    }
}
