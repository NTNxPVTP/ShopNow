package com.example.shopnow.product.buyer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.shopnow.product.models.Product;

interface ProductRepository extends JpaRepository<Product, UUID > {
    Optional<Product> findById(UUID idProduct);
}
    