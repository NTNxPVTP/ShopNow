package com.example.shopnow.product.buyer;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopnow.product.models.Product;
@Repository
interface ProductRepository extends JpaRepository<Product, UUID > {
    
}
    