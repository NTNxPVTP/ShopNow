package com.example.shopnow.cart.rest;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import com.example.shopnow.user.api.AuthUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.shopnow.cart.application.dto.CartItemRequest;
import com.example.shopnow.cart.application.dto.CartResponse;
import com.example.shopnow.cart.application.usecases.CartUseCase;
import com.example.shopnow.user.api.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartUseCase cartUseCase;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthUser AuthenticatedUser user) {
        return ResponseEntity.ok(cartUseCase.getCart(user));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
            @AuthUser AuthenticatedUser user,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartUseCase.addItem(user, request));
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthUser AuthenticatedUser user,
            @PathVariable UUID productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(cartUseCase.updateItemQuantity(user, productId, quantity));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthUser AuthenticatedUser user,
            @PathVariable UUID productId) {
        return ResponseEntity.ok(cartUseCase.removeItem(user, productId));
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthUser AuthenticatedUser user) {
        cartUseCase.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}

