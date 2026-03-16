package com.example.shopnow.product.seller.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopnow.product.models.Product;
@Repository
interface ProductSellerRepository extends JpaRepository<Product, UUID>{
    
}
