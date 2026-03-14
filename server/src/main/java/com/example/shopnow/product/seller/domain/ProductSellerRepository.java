package com.example.shopnow.product.seller.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.shopnow.product.seller.domain.models.Product;

interface ProductSellerRepository extends JpaRepository<UUID, Product>{

}
