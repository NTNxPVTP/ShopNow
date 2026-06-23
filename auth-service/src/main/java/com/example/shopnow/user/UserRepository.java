package com.example.shopnow.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.shopnow.user.models.User;
import java.util.Optional;


public interface UserRepository extends JpaRepository<User, UUID>{
    Optional<User> findByEmail(String email);
}
