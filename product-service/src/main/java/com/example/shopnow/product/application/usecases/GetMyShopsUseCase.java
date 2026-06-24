package com.example.shopnow.product.application.usecases;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.ShopRepository;
import com.example.shopnow.product.application.dto.ShopDTO;
import com.example.shopnow.product.domain.models.Shop;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMyShopsUseCase {

    private final ShopRepository shopRepository;

    @Transactional(readOnly = true)
    public List<ShopDTO> execute(AuthenticatedUser owner) {
        if (owner == null || owner.getId() == null) {
            throw new RuntimeException("Unauthorized");
        }
        
        List<Shop> shops = shopRepository.findByOwnerId(owner.getId());
        
        return shops.stream().map(shop -> new ShopDTO(
            shop.getId(),
            shop.getName(),
            shop.getAddress(),
            shop.getAvatarUrl(),
            shop.getIsActive(),
            shop.getOwnerId(),
            shop.getCreatedAt()
        )).collect(Collectors.toList());
    }
}
