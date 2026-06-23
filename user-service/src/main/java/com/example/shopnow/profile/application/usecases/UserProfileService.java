package com.example.shopnow.profile.application.usecases;

import com.example.shopnow.profile.application.dto.UpdateProfileRequest;
import com.example.shopnow.profile.application.dto.UserProfileDto;
import com.example.shopnow.profile.domain.models.UserProfile;
import com.example.shopnow.profile.domain.repositories.UserProfileRepository;
import com.example.shopnow.user.api.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AuthApiClient authApiClient;

    public UserProfileDto getProfile(AuthenticatedUser authUser) {
        UserProfile profile = userProfileRepository.findByUserId(authUser.getId())
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .userId(authUser.getId())
                                .build()
                ));

        AuthApiClient.UserInfoDto userInfo = authApiClient.getUserInfo(authUser.getId());

        return UserProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .email(userInfo != null ? userInfo.email() : null)
                .name(userInfo != null ? userInfo.name() : null)
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }

    public UserProfileDto updateProfile(AuthenticatedUser authUser, UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findByUserId(authUser.getId())
                .orElseGet(() -> UserProfile.builder().userId(authUser.getId()).build());

        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());

        profile = userProfileRepository.save(profile);

        AuthApiClient.UserInfoDto userInfo = authApiClient.getUserInfo(authUser.getId());

        return UserProfileDto.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .email(userInfo != null ? userInfo.email() : null)
                .name(userInfo != null ? userInfo.name() : null)
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .avatarUrl(profile.getAvatarUrl())
                .build();
    }
}
