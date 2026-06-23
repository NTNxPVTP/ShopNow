package com.example.shopnow.cart.api;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ProductApiClient {

    private final RestTemplate restTemplate;
    private final String productUrl;

    public ProductApiClient(RestTemplate restTemplate, @Value("${app.product.url:http://localhost:8082}") String productUrl) {
        this.restTemplate = restTemplate;
        this.productUrl = productUrl;
    }

    public ProductInfo getProductInfo(UUID productId) {
        HttpHeaders headers = new HttpHeaders();
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String authHeader = request.getHeader("X-Auth-User-Id");
            if (authHeader != null) {
                headers.set("X-Auth-User-Id", authHeader);
            }
        }

        ResponseEntity<ProductInfo> response = restTemplate.exchange(
                productUrl + "/api/products/" + productId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                ProductInfo.class
        );

        return response.getBody();
    }
}

