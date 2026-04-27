package com.example.shopnow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.parameters.P;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    //User Errors
    USER_NOT_FOUND("USER_001", "User Not Found", "The requested user does not exist", HttpStatus.NOT_FOUND),
    USERNAME_ALREADY_EXISTS("USER_002", "Username Already Exists", "The username is already taken", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS("USER_003", "Email Already Exists", "The email is already registered", HttpStatus.BAD_REQUEST),

    // Product Errors
    PRODUCT_NOT_FOUND("PRODUCT_001", "Product Not Found", "The requested product does not exist", HttpStatus.NOT_FOUND),
    PRODUCT_OUT_OF_STOCK("PRODUCT_002", "Product Out Of Stock", "The requested product is currently out of stock or has insufficient quantity", HttpStatus.BAD_REQUEST),
    SHOP_NOT_FOUND("PRODUCT_003", "Shop Not Found", "The requested shop does not exist", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("PRODUCT_004", "Category Not Found", "One or more requested categories do not exist", HttpStatus.NOT_FOUND),
    PRODUCT_ACCESS_DENIED("PRODUCT_005", "Access Denied", "You do not have permission to modify this product", HttpStatus.FORBIDDEN),
    CATEGORY_ALREADY_EXISTS("PRODUCT_006", "Category Already Exists", "The requested category name already exists", HttpStatus.BAD_REQUEST),

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
