package com.example.shopnow.product.infrastructure.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.product.application.dto.CreateShopRequest;
import com.example.shopnow.product.application.dto.ShopDTO;
import com.example.shopnow.product.application.usecases.CreateShopUseCase;
import com.example.shopnow.user.api.AuthUser;
import com.example.shopnow.user.api.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.shopnow.product.application.usecases.GetMyShopsUseCase;
import com.example.shopnow.product.application.usecases.GetShopUseCase;
import com.example.shopnow.product.application.usecases.GetShopProductsUseCase;
import com.example.shopnow.product.application.dto.ProductDetailResponse;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final CreateShopUseCase createShopUseCase;
    private final GetMyShopsUseCase getMyShopsUseCase;
    private final GetShopUseCase getShopUseCase;
    private final GetShopProductsUseCase getShopProductsUseCase;

    @PostMapping
    public ResponseEntity<ShopDTO> createShop(
            @Valid @RequestBody CreateShopRequest request,
            @AuthUser AuthenticatedUser owner) {
        return ResponseEntity.ok(createShopUseCase.execute(request, owner));
    }

    @GetMapping
    public ResponseEntity<List<ShopDTO>> getMyShops(@AuthUser AuthenticatedUser owner) {
        return ResponseEntity.ok(getMyShopsUseCase.execute(owner));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopDTO> getShop(@PathVariable UUID id) {
        return ResponseEntity.ok(getShopUseCase.execute(id));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductDetailResponse>> getShopProducts(@PathVariable UUID id) {
        return ResponseEntity.ok(getShopProductsUseCase.execute(id));
    }
}
