package com.example.shopnow.product.seller.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.seller.rest.dto.ProductDetailResponse;
import com.example.shopnow.shared.DomainException;
import com.example.shopnow.shared.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class ProductSellerService {
    private final ProductSellerRepository repository;
    private final ProductMapper mapper;

    public ProductDetailResponse getDetailProduct(UUID id){
        Product product = repository.findById(id)
                            .orElseThrow(() ->  new DomainException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapper.toDetailResponse(product);
    }
}
