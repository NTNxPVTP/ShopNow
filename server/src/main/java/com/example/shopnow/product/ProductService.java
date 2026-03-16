package com.example.shopnow.product;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.rest.dto.CreateProductRequest;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@Transactional( readOnly= true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    public Product viewDetailsOfProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(()-> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return product;

    }

    //has not have shop owner, category,... yet
    @Transactional
    public ProductDetailResponse createProduct(CreateProductRequest request){
        System.out.println("This is a request");
        System.out.println(request);
        
        Product product = mapper.fromRequestToProduct(request);
        product.setStatus(ProductStatus.ACTIVE);
        product = productRepository.save(product);
        ProductDetailResponse detail = mapper.toDetailResponse(product);
        return detail;
    }
}
