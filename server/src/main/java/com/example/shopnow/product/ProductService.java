package com.example.shopnow.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {
    @Autowired
    ProductRepository productRepository;
    public Product viewDetailsOfProduct(Integer id) {
        return productRepository.findByIdProduct(id);
    }
}
