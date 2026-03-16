package com.example.shopnow.product;

import org.springframework.stereotype.Component;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;

@Component
class ProductMapper {
    public ProductDetailResponse toDetailResponse(Product product){
        return new ProductDetailResponse(
            product.getId(),
            product.getName(),
            product.getPictureUrl(),
            product.getQuantity(),
            product.getPrice(),
            product.getStatus());
    }
    public Product fromRequestToProduct(CreateProductRequest request){
        System.out.println(request);
        return Product.builder()
                    .name(request.name())
                    .pictureUrl(request.pictureUrl())
                    .price(request.price())
                    .quantity(request.quantity())
                    .build();
    }
}
