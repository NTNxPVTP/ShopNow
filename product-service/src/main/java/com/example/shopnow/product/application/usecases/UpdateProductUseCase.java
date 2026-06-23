package com.example.shopnow.product.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.ProductMapper;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.application.dto.UpdateProductRequest;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.repository.ProductRepository;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that updates a product owned by the authenticated
 * shop owner.
 *
 * <p>Exposes a single write execution entry point,
 * {@link #execute(UpdateProductRequest, UUID, AuthenticatedUser)}. It loads the
 * product with its shop, enforces the owner-equals-shop-owner authorization
 * check, applies the partial update via the mapper and persists through the
 * {@link ProductRepository} port — preserving the exact behavior previously
 * implemented in {@code ProductServiceImpl.updateProduct}. The mapper already
 * handles any price field on the update request, so behavior is preserved
 * without forcing {@code Product.changePrice}.
 */
@Service
@RequiredArgsConstructor
public class UpdateProductUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional(rollbackFor = Exception.class)
    public ProductDetailResponse execute(UpdateProductRequest request, UUID productId, AuthenticatedUser owner) {
        UUID shopOwnerId = owner.getId();
        Product product = productRepository.findWithShopById(productId)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        if (shopOwnerId == null || !shopOwnerId.equals(product.getShop().getOwnerId())) {
            throw new DomainException(ErrorCode.PRODUCT_ACCESS_DENIED);
        }
        productMapper.updateProductFromUpdateRequest(request, product);
        productRepository.save(product);
        return productMapper.toDto(product);
    }
}
