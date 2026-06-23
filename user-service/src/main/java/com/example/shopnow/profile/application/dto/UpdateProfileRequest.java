package com.example.shopnow.profile.application.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String phone;
    private String address;
    private String avatarUrl;
}
