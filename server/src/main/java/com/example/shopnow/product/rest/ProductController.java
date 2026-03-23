package com.example.shopnow.product.rest;

import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.product.ProductService;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.PageResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> viewDetailsOfProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.viewDetailsOfProduct(id));

    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> createProduct(@RequestBody @Valid CreateProductRequest request){
        ProductDetailResponse detail = productService.createProduct(request);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable UUID id){
        return ResponseEntity.ok( productService.deleteProduct(id));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
        @PathVariable UUID id,
        @RequestBody UpdateProductRequest request
    ){
        return ResponseEntity.ok(productService.updateProduct(request, id));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductDetailResponse>> getProducts(
        @RequestParam(required = false, defaultValue = "1") int page
    ){
        Pageable pageable = PageRequest.of(page-1, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(productService.getProducts(pageable));
    }
}
