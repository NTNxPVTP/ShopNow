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

@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final CreateShopUseCase createShopUseCase;

    @PostMapping
    public ResponseEntity<ShopDTO> createShop(
            @Valid @RequestBody CreateShopRequest request,
            @AuthUser AuthenticatedUser owner) {
        return ResponseEntity.ok(createShopUseCase.execute(request, owner));
    }
}
