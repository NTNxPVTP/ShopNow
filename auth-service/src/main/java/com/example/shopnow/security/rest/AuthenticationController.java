package com.example.shopnow.security.rest;

import org.springframework.web.bind.annotation.RestController;
import com.example.shopnow.security.AuthenticationService;
import com.example.shopnow.security.rest.dto.AuthenticationRequest;
import com.example.shopnow.security.rest.dto.AuthenticationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
        
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody com.example.shopnow.security.rest.dto.RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }
    
}
