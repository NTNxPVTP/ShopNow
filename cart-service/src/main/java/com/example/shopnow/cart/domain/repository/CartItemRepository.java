package com.example.shopnow.cart.domain.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.cart.domain.models.CartItem;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {
}
