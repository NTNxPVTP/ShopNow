package com.example.shopnow.product.rest;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.product.ProductService;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.product.rest.dto.UpdateProductRequest;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.User;

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
    public ResponseEntity<ProductDetailResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request,
            @AuthenticationPrincipal User owner) {
        ProductDetailResponse detail = productService.createProduct(request, owner);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable UUID id, @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(productService.deleteProduct(id, owner));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(productService.updateProduct(request, id, owner));
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductDetailResponse>> getProducts(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) UUID shopId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") boolean inStockOnly) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(productService.getProducts(
                pageable,
                shopId,
                categoryId,
                keyword,
                minPrice,
                maxPrice,
                inStockOnly));
    }
}
