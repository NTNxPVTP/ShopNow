package com.example.shopnow.profile.domain.models;

import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(name = "user_profiles")
public class UserProfile extends BaseEntity {

    private UUID userId;
    private String phone;
    private String address;
    private String avatarUrl;

}
