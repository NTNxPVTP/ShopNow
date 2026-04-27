package com.example.shopnow.product;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopnow.product.models.Shop;

@Repository
public interface ShopRepository extends JpaRepository<Shop, UUID> {
}
