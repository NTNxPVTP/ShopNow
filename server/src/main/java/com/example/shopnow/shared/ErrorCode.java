package com.example.shopnow.shared;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Product Errors
    PRODUCT_NOT_FOUND("PRODUCT_001", "Product Not Found", "The requested product does not exist", HttpStatus.NOT_FOUND),

    // Order Errors
    ORDER_NOT_FOUND("ORDER_001", "Order Not Found", "The requested order does not exist", HttpStatus.NOT_FOUND),

    // Generic Erors
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized Error", "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR);
    private final String code;
    private final String title;
    private final String message;
    private final HttpStatus status;
}
