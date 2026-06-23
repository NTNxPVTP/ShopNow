package com.example.shopnow.user.api;

import com.example.shopnow.user.models.User;
import com.example.shopnow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/internal/auth")
@RequiredArgsConstructor
public class AuthInternalController {

    private final UserRepository userRepository;

    @GetMapping("/users/{userId}")
    public ResponseEntity<UserInfoDto> getUserInfo(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(new UserInfoDto(user.getId(), user.getEmail(), user.getName())))
                .orElse(ResponseEntity.notFound().build());
    }

    public record UserInfoDto(UUID id, String email, String name) {}
}
