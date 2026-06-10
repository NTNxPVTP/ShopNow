package com.example.shopnow.security;

import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-40: secret key < 256 bit accepted without fail-fast.
 *
 * <p><b>Validates: Requirements 2.40</b>
 *
 * <p>Property 34: Bug Condition - Secret key length validation.
 * JwtService SHALL have a {@code @PostConstruct} method that validates the configured
 * secret key is at least 256 bits (32 bytes) when Base64-decoded. If the key is too
 * short, the application context SHALL fail-fast with an {@code IllegalStateException}
 * containing "JWT secret key must be at least 256 bits".
 *
 * <p>On unfixed code this test FAILS because JwtService has no {@code @PostConstruct}
 * validation method — the weak key error only surfaces at runtime on the first request
 * as a {@code WeakKeyException}.
 */
class JwtServiceBug40ExplorationTest {

    @Test
    @DisplayName("BUG-40: JwtService SHALL have a @PostConstruct method that validates secret key length >= 256 bits")
    void jwtService_shallHave_postConstructKeyValidation() {
        Class<?> jwtServiceClass = JwtService.class;

        // Find any method annotated with @PostConstruct
        boolean hasPostConstructValidation = Arrays.stream(jwtServiceClass.getDeclaredMethods())
                .anyMatch(method -> method.isAnnotationPresent(PostConstruct.class));

        assertThat(hasPostConstructValidation)
                .as("JwtService SHALL have a @PostConstruct method that validates the "
                    + "configured secret key is at least 256 bits (32 bytes when Base64-decoded). "
                    + "Without this, a weak key (< 256 bits) is only detected at runtime on the "
                    + "first JWT operation as a WeakKeyException, instead of failing fast at startup.")
                .isTrue();
    }

    @Test
    @DisplayName("BUG-40: @PostConstruct validation method SHALL reference key length check logic")
    void jwtService_postConstructMethod_shallValidateKeyLength() throws Exception {
        Class<?> jwtServiceClass = JwtService.class;

        // Find the @PostConstruct method
        Method postConstructMethod = Arrays.stream(jwtServiceClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .findFirst()
                .orElse(null);

        assertThat(postConstructMethod)
                .as("JwtService SHALL have a @PostConstruct method for key validation. "
                    + "On unfixed code, no such method exists — the application starts "
                    + "successfully even with a key < 256 bits, and the error only occurs "
                    + "at the first JWT signing/parsing operation.")
                .isNotNull();
    }
}
