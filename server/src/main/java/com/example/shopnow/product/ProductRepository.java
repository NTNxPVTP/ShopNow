package com.example.shopnow.product;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Integer > {
    public Product findByIdProduct(Integer idProduct);
}
