package com.example.shopnow.product.application.usecases;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.infrastructure.persistence.CategoryRepository;
import com.example.shopnow.product.ProductMapper;
import com.example.shopnow.product.ShopRepository;
import com.example.shopnow.product.application.dto.CreateProductRequest;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.domain.models.Category;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.models.Shop;
import com.example.shopnow.product.domain.repository.ProductRepository;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that creates a product owned by the authenticated
 * shop owner.
 *
 * <p>Exposes a single write execution entry point,
 * {@link #execute(CreateProductRequest, AuthenticatedUser)}. It orchestrates
 * the shop lookup, the owner-equals-shop-owner authorization check, category
 * resolution, and persistence through the {@link ProductRepository} port,
 * preserving the exact behavior previously implemented in
 * {@code ProductServiceImpl.createProduct}.
 */
@Service
@RequiredArgsConstructor
public class CreateProductUseCase {

    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse execute(CreateProductRequest request, UUID shopId, AuthenticatedUser owner) {
        Product product = productMapper.fromCreateRequestToProduct(request);
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new DomainException(ErrorCode.SHOP_NOT_FOUND));

        UUID shopOwnerId = owner.getId();
        if (shopOwnerId == null || !shopOwnerId.equals(shop.getOwnerId())) {
            throw new DomainException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }

        Set<Category> categories = resolveCategories(request.categoryIds());

        product.setShop(shop);
        product.setStatus(ProductStatus.ACTIVE);

        if (!categories.isEmpty()) {
            product.setCategories(categories);
        }

        product = productRepository.save(product);
        return productMapper.toDto(product);
    }

    private Set<Category> resolveCategories(Set<UUID> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>(categoryIds);
        Set<Category> categories = categoryRepository.findAllByIdIn(uniqueIds);
        if (categories.size() != uniqueIds.size()) {
            throw new DomainException(ErrorCode.CATEGORY_NOT_FOUND);
        }

        return categories;
    }
}
