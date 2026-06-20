package com.example.shopnow.product.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.product.ProductMapper;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that returns the detail of a single, currently
 * {@code ACTIVE} and non-deleted product.
 *
 * <p>Exposes a single read-only execution entry point, {@link #execute(UUID)}.
 * It looks the product up through the framework-neutral
 * {@link ProductRepository} port and throws {@link ErrorCode#PRODUCT_NOT_FOUND}
 * when it is missing, preserving the exact behavior previously implemented in
 * {@code ProductServiceImpl.viewDetailsOfProduct}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class ViewProductDetailUseCase {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductDetailResponse execute(UUID id) {
        Product product = productRepository.findActiveById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toDto(product);
    }
}
