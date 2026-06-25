package com.example.shopnow.product.api;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.example.shopnow.product.api.dto.OrderLineRequest;
import com.example.shopnow.product.api.dto.ProductInfoForOrder;

@Service
public class ProductApiClient implements ProductApi {

    private final RestTemplate restTemplate;
    private final String productUrl;

    public ProductApiClient(RestTemplate restTemplate, @Value("${app.product.url:http://localhost:8082}") String productUrl) {
        this.restTemplate = restTemplate;
        this.productUrl = productUrl;
    }

    @Override
    public List<ProductInfoForOrder> decreaseProducts(List<OrderLineRequest> lines) {
        try {
            ResponseEntity<List<ProductInfoForOrder>> response = restTemplate.exchange(
                    productUrl + "/api/internal/products/decrease",
                    HttpMethod.POST,
                    new HttpEntity<>(lines),
                    new ParameterizedTypeReference<List<ProductInfoForOrder>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseString = e.getResponseBodyAsString();
            if (responseString.contains("PRODUCT_002") || responseString.contains("PRODUCT_OUT_OF_STOCK")) {
                throw new com.example.shopnow.exception.DomainException(com.example.shopnow.exception.ErrorCode.PRODUCT_OUT_OF_STOCK);
            } else if (responseString.contains("PRODUCT_001") || responseString.contains("PRODUCT_NOT_FOUND")) {
                throw new com.example.shopnow.exception.DomainException(com.example.shopnow.exception.ErrorCode.PRODUCT_NOT_FOUND);
            }
            throw new RuntimeException("Error from product-service: " + e.getMessage() + " - " + responseString);
        }
    }

    @Override
    public List<ProductInfoForOrder> getProductsInfo(List<OrderLineRequest> lines) {
        try {
            ResponseEntity<List<ProductInfoForOrder>> response = restTemplate.exchange(
                    productUrl + "/api/internal/products/info",
                    HttpMethod.POST,
                    new HttpEntity<>(lines),
                    new ParameterizedTypeReference<List<ProductInfoForOrder>>() {}
            );
            return response.getBody();
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseString = e.getResponseBodyAsString();
            if (responseString.contains("PRODUCT_002") || responseString.contains("PRODUCT_OUT_OF_STOCK")) {
                throw new com.example.shopnow.exception.DomainException(com.example.shopnow.exception.ErrorCode.PRODUCT_OUT_OF_STOCK);
            } else if (responseString.contains("PRODUCT_001") || responseString.contains("PRODUCT_NOT_FOUND")) {
                throw new com.example.shopnow.exception.DomainException(com.example.shopnow.exception.ErrorCode.PRODUCT_NOT_FOUND);
            }
            throw new RuntimeException("Error from product-service: " + e.getMessage() + " - " + responseString);
        }
    }
}

