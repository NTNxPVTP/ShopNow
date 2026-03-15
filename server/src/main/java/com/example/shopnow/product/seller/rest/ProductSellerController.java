package com.example.shopnow.product.seller.rest;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.product.seller.domain.ProductSellerService;
import com.example.shopnow.product.seller.rest.dto.ProductDetailResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seller/products")
@RequiredArgsConstructor
public class ProductSellerController {
    private final ProductSellerService service;
    
    @GetMapping("/{productId}")
    public ResponseEntity<ProductDetailResponse> getProductSeller(@PathVariable UUID productId){
        return ResponseEntity.ok(service.getDetailProduct(productId));
    }
}
