package com.example.shopnow.exception;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // Product Errors
    PRODUCT_NOT_FOUND("PRODUCT_001", "Product Not Found", "The requested product does not exist", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK("PRODUCT_002", "Product Out Of Stock", "The requested product is currently out of stock or has insufficient quantity", HttpStatus.BAD_REQUEST),

    // Order Errors
    ORDER_NOT_FOUND("ORDER_001", "Order Not Found", "The requested order does not exist", HttpStatus.NOT_FOUND),
    INSUFFICIENT_STOCK("ORDER_002", "Insufficient Stock", "The requested product quantity is not enough", HttpStatus.BAD_REQUEST),
    ORDER_ACCESS_DENIED("ORDER_003", "Access Denied", "You do not have permission to view this order", HttpStatus.FORBIDDEN),
    
    // Generic Erors
    UNCATEGORIZED_EXCEPTION("9999", "Uncategorized Error", "An unexpected error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR);
    private final String code;
    private final String title;
    private final String message;
    private final HttpStatus status;
}
