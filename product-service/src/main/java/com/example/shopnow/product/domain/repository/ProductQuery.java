package com.example.shopnow.product.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.shopnow.product.domain.models.ProductStatus;

/**
 * Pure query object carrying the dynamic filter criteria used when listing or
 * paginating products. It replaces the leakage of Spring Data
 * {@code Specification} into the {@code application} and {@code domain} layers:
 * the application layer builds a {@link ProductQuery} and the persistence
 * adapter translates it into a {@code Specification} internally.
 *
 * <p>This is a framework-neutral domain value type — it intentionally carries no
 * Spring Data ({@code org.springframework.data.*}) nor JPA
 * ({@code jakarta.persistence.*}) types.
 *
 * @param shopId      filter by shop, or {@code null} to ignore
 * @param categoryId  filter by category, or {@code null} to ignore
 * @param keyword     case-insensitive name search, or {@code null}/blank to ignore
 * @param minPrice    inclusive lower price bound, or {@code null} to ignore
 * @param maxPrice    inclusive upper price bound, or {@code null} to ignore
 * @param status      filter by product status, or {@code null} to ignore
 * @param inStockOnly when {@code true}, only products with {@code quantity > 0}
 */
public record ProductQuery(
        UUID shopId,
        UUID categoryId,
        String keyword,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        ProductStatus status,
        boolean inStockOnly) {
}
