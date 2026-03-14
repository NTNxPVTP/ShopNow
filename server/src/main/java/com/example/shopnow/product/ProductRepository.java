package com.example.shopnow.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Integer > {
    public Product findByIdProduct(Integer idProduct);
}
    