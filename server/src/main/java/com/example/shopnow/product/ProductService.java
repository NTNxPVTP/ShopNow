package com.example.shopnow.product;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.rest.dto.ProductDetailResponse;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@Transactional( readOnly= true, rollbackFor = Exception.class)
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    public ProductDetailResponse viewDetailsOfProduct(UUID id) {

        Product product = productRepository.findById(id)
                .orElseThrow(()-> new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toDetailResponse(product);
    }

    // public ProductDetail createProduct(CreateProductRequest request){
    //     Product product = ProductMapper.fromRequestToProduct(request);
    //     product = productRepository.save(product);
    //     ProductDetail detail = ProductMapper.toDetail(product);
    //     return detail;
    // }
}
