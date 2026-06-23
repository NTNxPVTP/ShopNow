package com.example.shopnow.payment.rest;

import org.springframework.http.ResponseEntity;
import com.example.shopnow.user.api.AuthUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.shopnow.payment.application.dto.PaymentRequest;
import com.example.shopnow.payment.application.dto.PaymentResponse;
import com.example.shopnow.payment.application.usecases.PaymentUseCase;
import com.example.shopnow.user.api.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentUseCase paymentUseCase;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @AuthUser AuthenticatedUser user,
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentUseCase.processPayment(user, request));
    }
}

