package com.example.shopnow.product.application.usecases;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetShopProductsUseCase {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductDetailResponse> execute(UUID shopId) {
        List<Product> products = productRepository.findByShopId(shopId);
        
        return products.stream().map(product -> new ProductDetailResponse(
            product.getId(),
            product.getName(),
            product.getPictureUrl(),
            product.getQuantity(),
            product.getPrice(),
            product.getStatus(),
            product.getShop() != null ? product.getShop().getId() : null,
            product.getCategories().stream()
                .map(com.example.shopnow.product.domain.models.Category::getId)
                .collect(Collectors.toSet())
        )).collect(Collectors.toList());
    }
}
