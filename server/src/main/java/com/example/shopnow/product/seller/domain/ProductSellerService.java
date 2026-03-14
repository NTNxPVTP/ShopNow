package com.example.shopnow.product.seller.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductSellerService {
    private final ProductSellerRepository repository;

    public
}
