package com.example.shopnow.product.seller.domain;

import org.springframework.stereotype.Component;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.seller.rest.dto.ProductDetailResponse;

@Component
class ProductMapper {
    public ProductDetailResponse toDetailResponse(Product product){
        return new ProductDetailResponse(
            product.getId(),
            product.getName(),
            product.getPicture_url(),
            product.getQuantity(),
            product.getPrice(),
            product.getStatus());
    }
}
