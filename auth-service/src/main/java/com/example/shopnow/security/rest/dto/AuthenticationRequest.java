package com.example.shopnow.security.rest.dto;

import lombok.Builder;

@Builder
public record AuthenticationRequest(
    String email,
    String password
) {} 
