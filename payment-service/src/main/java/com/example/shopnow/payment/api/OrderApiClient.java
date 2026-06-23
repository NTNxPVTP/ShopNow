package com.example.shopnow.payment.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.shared.PageResponse;

import java.util.UUID;

@Component
public class OrderApiClient {

    private final RestTemplate restTemplate;

    @Value("${app.order.url:http://localhost:8081}")
    private String orderUrl;

    public OrderApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public OrderInfo getOrderInfo(UUID orderId) {
        try {
            ResponseEntity<OrderInfo> response = restTemplate.exchange(
                    orderUrl + "/api/internal/orders/" + orderId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<OrderInfo>() {
                    });
            return response.getBody();
        } catch (Exception e) {
            System.err.println("❌ THẤT BẠI KHI GỌI SANG ORDER SERVICE: " + e.getMessage());
            throw new DomainException(ErrorCode.ORDER_NOT_FOUND);
        }
    }
}
