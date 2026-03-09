package com.example.shopnow.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional( readOnly= true, rollbackFor = Exception.class)
public class ProductService {
    @Autowired
    ProductRepository productRepository;
    public Product viewDetailsOfProduct(Integer id) {
        return productRepository.findByIdProduct(id);
    }

    // public ProductDetail createProduct(CreateProductRequest request){
    //     Product product = ProductMapper.fromRequestToProduct(request);
    //     product = productRepository.save(product);
    //     ProductDetail detail = ProductMapper.toDetail(product);
    //     return detail;
    // }
}
