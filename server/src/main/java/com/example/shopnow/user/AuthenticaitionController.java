package com.example.shopnow.user;

import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.user.models.AuthenticationRequest;
import com.example.shopnow.user.models.AuthenticationResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequiredArgsConstructor
public class AuthenticaitionController {
    private final AuthenticationService authenticationService;
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }
    
}
