package com.example.shopnow.product.application.usecases;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.ProductMapper;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.repository.ProductQuery;
import com.example.shopnow.product.infrastructure.persistence.ProductJpaRepository;
import com.example.shopnow.shared.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that lists {@code ACTIVE}, non-deleted products,
 * paginated and filtered.
 *
 * <p>It expresses its filter purely as a framework-neutral {@link ProductQuery}
 * (carrying {@code ACTIVE} status and the {@code inStockOnly} flag) and never
 * builds a Spring Data {@code Specification} itself — the {@code Specification}
 * translation stays confined to {@code infrastructure/persistence} via
 * {@link ProductJpaRepository#search(ProductQuery, Pageable)}, which composes
 * exactly the previous predicate set (status, shop, category, keyword, price
 * range, {@code isNotDeleted}, and the conditional {@code isInStock}). This
 * preserves the exact paged behavior previously implemented in
 * {@code ProductServiceImpl.getProducts}.
 *
 * <p>Exposes a single read-only execution entry point, {@link #execute}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class ListProductsUseCase {

    private final ProductJpaRepository productJpaRepository; // infrastructure paging search
    private final ProductMapper productMapper;

    public PageResponse<ProductDetailResponse> execute(
            Pageable pageable,
            UUID shopId,
            UUID categoryId,
            String keyword,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            boolean inStockOnly) {

        // Application layer only constructs a framework-neutral query object;
        // the Specification is built inside the persistence layer.
        ProductQuery query = new ProductQuery(
                shopId,
                categoryId,
                keyword,
                minPrice,
                maxPrice,
                ProductStatus.ACTIVE,
                inStockOnly);

        Page<Product> products = productJpaRepository.search(query, pageable);
        return productMapper.toPageResponse(products);
    }
}
