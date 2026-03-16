package com.example.shopnow.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.product.models.Product;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID > {

}
    