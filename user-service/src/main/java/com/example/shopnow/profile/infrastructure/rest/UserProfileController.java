package com.example.shopnow.profile.infrastructure.rest;

import com.example.shopnow.profile.application.dto.UpdateProfileRequest;
import com.example.shopnow.profile.application.dto.UserProfileDto;
import com.example.shopnow.profile.application.usecases.UserProfileService;
import com.example.shopnow.user.api.AuthUser;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthUser AuthenticatedUser user) {
        return ResponseEntity.ok(userProfileService.getProfile(user));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateMyProfile(
            @AuthUser AuthenticatedUser user,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(user, request));
    }
}
