package com.example.shopnow.shared;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException{
    private final ErrorCode errorCode;
    public DomainException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
