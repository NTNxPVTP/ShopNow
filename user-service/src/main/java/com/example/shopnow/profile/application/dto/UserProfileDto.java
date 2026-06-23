package com.example.shopnow.profile.application.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class UserProfileDto {
    private UUID id;
    private UUID userId;
    private String email; // From auth-service
    private String name;  // From auth-service
    private String phone;
    private String address;
    private String avatarUrl;
}
