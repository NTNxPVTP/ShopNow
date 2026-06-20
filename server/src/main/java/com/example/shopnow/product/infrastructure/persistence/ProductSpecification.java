package com.example.shopnow.product.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.repository.ProductQuery;

/**
 * Product {@code Specification} factory, confined to the
 * {@code infrastructure/persistence} package so Spring Data
 * {@code Specification} never leaks into the {@code domain} or
 * {@code application} layers (Requirement 4.6). The application layer builds a
 * framework-neutral {@link ProductQuery} and the persistence adapter translates
 * it into a {@code Specification} via {@link #from(ProductQuery)}.
 */
public class ProductSpecification {

    /**
     * Builds a composite {@link Specification} from a pure {@link ProductQuery}
     * by composing the status, shop, category, keyword, price-range and
     * in-stock predicates. A {@code null} query is treated as match-all.
     *
     * <p>This keeps Spring Data {@code Specification} confined to the
     * {@code infrastructure/persistence} package: the application layer passes
     * a {@link ProductQuery} and the persistence adapter translates it here.
     */
    public static Specification<Product> from(ProductQuery query) {
        if (query == null) {
            return Specification.allOf(isNotDeleted());
        }
        Specification<Product> spec = Specification.allOf(
                hasStatus(query.status()),
                hasShopId(query.shopId()),
                hasCategoryId(query.categoryId()),
                hasNameLike(query.keyword()),
                hasPriceGreaterThanOrEqual(query.minPrice()),
                hasPriceLessThanOrEqual(query.maxPrice()),
                isNotDeleted());
        if (query.inStockOnly()) {
            spec = spec.and(isInStock());
        }
        return spec;
    }

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
