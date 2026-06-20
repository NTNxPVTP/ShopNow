package com.example.shopnow.product.infrastructure.rest;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.product.application.dto.CreateProductRequest;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import com.example.shopnow.product.application.dto.UpdateProductRequest;
import com.example.shopnow.product.application.usecases.CreateProductUseCase;
import com.example.shopnow.product.application.usecases.DeleteProductUseCase;
import com.example.shopnow.product.application.usecases.ListProductsUseCase;
import com.example.shopnow.product.application.usecases.UpdateProductUseCase;
import com.example.shopnow.product.application.usecases.ViewProductDetailUseCase;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ViewProductDetailUseCase viewProductDetailUseCase;
    private final CreateProductUseCase createProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ListProductsUseCase listProductsUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> viewDetailsOfProduct(@PathVariable UUID id) {
        return ResponseEntity.ok(viewProductDetailUseCase.execute(id));

    }

    @PostMapping
    public ResponseEntity<ProductDetailResponse> createProduct(
            @RequestBody @Valid CreateProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser owner) {
        ProductDetailResponse detail = createProductUseCase.execute(request, owner);
        return ResponseEntity.ok(detail);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser owner) {
        return ResponseEntity.ok(deleteProductUseCase.execute(id, owner));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDetailResponse> updateProduct(
            @PathVariable UUID id,
            @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal AuthenticatedUser owner) {
        return ResponseEntity.ok(updateProductUseCase.execute(request, id, owner));
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
        return ResponseEntity.ok(listProductsUseCase.execute(
                pageable,
                shopId,
                categoryId,
                keyword,
                minPrice,
                maxPrice,
                inStockOnly));
    }
}
