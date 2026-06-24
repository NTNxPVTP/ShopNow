package com.example.shopnow.product.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.ShopRepository;
import com.example.shopnow.product.application.dto.ShopDTO;
import com.example.shopnow.product.domain.models.Shop;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetShopUseCase {

    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public ShopDTO execute(UUID shopId) {
        Shop shop = shopRepository.findById(shopId)
            .orElseThrow(() -> new RuntimeException("Shop not found"));
            
        return new ShopDTO(
            shop.getId(),
            shop.getName(),
            shop.getAddress(),
            shop.getAvatarUrl(),
            shop.getIsActive(),
            shop.getOwnerId(),
            shop.getCreatedAt()
        );
    }
}
