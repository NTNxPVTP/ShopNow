package com.example.shopnow.product;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID > {
    public Product findByIdProduct(UUID id);

}
    