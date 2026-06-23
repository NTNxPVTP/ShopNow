package com.example.shopnow.cart.application.usecases;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.cart.api.ProductApiClient;
import com.example.shopnow.cart.api.ProductInfo;
import com.example.shopnow.cart.application.dto.CartItemRequest;
import com.example.shopnow.cart.application.dto.CartItemResponse;
import com.example.shopnow.cart.application.dto.CartResponse;
import com.example.shopnow.cart.domain.models.Cart;
import com.example.shopnow.cart.domain.models.CartItem;
import com.example.shopnow.cart.domain.repository.CartItemRepository;
import com.example.shopnow.cart.domain.repository.CartRepository;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartUseCase {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductApiClient productApiClient;

    @Transactional
    public CartResponse getCart(AuthenticatedUser user) {
        Cart cart = getOrCreateCart(user.getId());
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse addItem(AuthenticatedUser user, CartItemRequest request) {
        // Validate product exists
        ProductInfo productInfo = getProductSafely(request.getProductId());

        Cart cart = getOrCreateCart(user.getId());
        
        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
        } else {
            CartItem newItem = new CartItem(cart, request.getProductId(), request.getQuantity(), java.time.LocalDateTime.now());
            cart.getItems().add(newItem);
        }

        cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(AuthenticatedUser user, UUID productId, Integer newQuantity) {
        Cart cart = getOrCreateCart(user.getId());
        
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));

        if (newQuantity <= 0) {
            cart.getItems().remove(existingItem);
            cartItemRepository.delete(existingItem);
        } else {
            existingItem.setQuantity(newQuantity);
        }

        cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(AuthenticatedUser user, UUID productId) {
        Cart cart = getOrCreateCart(user.getId());
        
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));

        cartRepository.save(cart);
        return mapToResponse(cart);
    }

    @Transactional
    public void clearCart(AuthenticatedUser user) {
        Cart cart = getOrCreateCart(user.getId());
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(UUID userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(new Cart(userId, new java.util.ArrayList<>(), null, null)));
    }

    private ProductInfo getProductSafely(UUID productId) {
        try {
            ProductInfo info = productApiClient.getProductInfo(productId);
            if (info == null) {
                throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            return info;
        } catch (Exception e) {
            throw new DomainException(ErrorCode.PRODUCT_NOT_FOUND);
        }
    }

    private CartResponse mapToResponse(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        List<CartItemResponse> itemResponses = new java.util.ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductInfo productInfo;
            try {
                productInfo = productApiClient.getProductInfo(item.getProductId());
            } catch (Exception e) {
                continue; // Skip items that don't exist anymore
            }
            
            if (productInfo != null) {
                itemResponses.add(new CartItemResponse(
                        item.getId(),
                        item.getProductId(),
                        productInfo.name(),
                        productInfo.price(),
                        item.getQuantity()
                ));
                total = total.add(productInfo.price().multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }

        return new CartResponse(cart.getId(), cart.getUserId(), itemResponses, total);
    }
}
