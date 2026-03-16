package com.example.shopnow.product.buyer;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
<<<<<<< Updated upstream:server/src/main/java/com/example/shopnow/product/buyer/ProductService.java
import com.example.shopnow.product.models.Product;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;
=======
import java.util.UUID;
import lombok.RequiredArgsConstructor;
>>>>>>> Stashed changes:server/src/main/java/com/example/shopnow/product/ProductService.java

@Service
@Transactional( readOnly= true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {
<<<<<<< Updated upstream:server/src/main/java/com/example/shopnow/product/buyer/ProductService.java
    @Autowired
    ProductRepository productRepository;
    public Product viewDetailsOfProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return product;
=======
    
    private final ProductRepository productRepository;
    public Product viewDetailsOfProduct(UUID id) {
        return productRepository.findByIdProduct(id);
>>>>>>> Stashed changes:server/src/main/java/com/example/shopnow/product/ProductService.java
    }

    // public ProductDetail createProduct(CreateProductRequest request){
    //     Product product = ProductMapper.fromRequestToProduct(request);
    //     product = productRepository.save(product);
    //     ProductDetail detail = ProductMapper.toDetail(product);
    //     return detail;
    // }
}
