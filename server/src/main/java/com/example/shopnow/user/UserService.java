package com.example.shopnow.user;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.shopnow.user.models.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public Optional<User> findByEmail(String email){
        return repository.findByEmail(email);
    }
}
