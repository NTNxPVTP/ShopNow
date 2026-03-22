package com.example.shopnow.exception;

import java.time.Instant;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler{
    
    private ResponseEntity<ProblemDetail> buildProblemDetail(ErrorCode errorCode){
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                errorCode.getStatus(),
                errorCode.getMessage());
        problemDetail.setTitle(errorCode.getTitle());
        problemDetail.setProperty("errorCode", errorCode.getCode());
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(errorCode.getStatus()).body(problemDetail);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ProblemDetail> handleDomainException(DomainException ex){
        return buildProblemDetail(ex.getErrorCode());
    }
}
