package com.example.shopnow.profile.application.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthApiClient {

    private final RestTemplate restTemplate;

    @Value("${app.auth.url:http://localhost:8081}")
    private String authUrl;

    public UserInfoDto getUserInfo(UUID userId) {
        try {
            ResponseEntity<UserInfoDto> response = restTemplate.getForEntity(
                    authUrl + "/api/internal/auth/users/" + userId,
                    UserInfoDto.class
            );
            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    public record UserInfoDto(UUID id, String email, String name) {}
}
