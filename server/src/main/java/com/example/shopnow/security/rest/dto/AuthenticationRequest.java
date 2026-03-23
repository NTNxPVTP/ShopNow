package com.example.shopnow.user.models;

import lombok.Builder;

@Builder
public record AuthenticationRequest(
    String email,
    String password
) {} 
