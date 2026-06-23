package com.example.shopnow.product.application.usecases;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.ShopRepository;
import com.example.shopnow.product.application.dto.CreateShopRequest;
import com.example.shopnow.product.application.dto.ShopDTO;
import com.example.shopnow.product.domain.models.Shop;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateShopUseCase {

    private final ShopRepository shopRepository;

    @Transactional(rollbackFor = Exception.class)
    public ShopDTO execute(CreateShopRequest request, AuthenticatedUser owner) {
        if (owner == null || owner.getId() == null) {
            throw new RuntimeException("Unauthorized");
        }
        
        Shop shop = new Shop();
        shop.setName(request.name());
        shop.setAddress(request.address());
        shop.setAvatarUrl(request.avatarUrl());
        shop.setIsActive(true);
        shop.setOwnerId(owner.getId());

        Shop saved = shopRepository.save(shop);
        
        return new ShopDTO(
            saved.getId(),
            saved.getName(),
            saved.getAddress(),
            saved.getAvatarUrl(),
            saved.getIsActive(),
            saved.getOwnerId(),
            saved.getCreatedAt()
        );
    }
}
