package com.example.shopnow.security;

import com.example.shopnow.security.models.Token;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-19: Token.user field is declared public, breaking encapsulation.
 *
 * Validates: Requirements 2.19
 *
 * Property 19: Token.user encapsulated.
 * Reflection check: Token.class.getDeclaredField("user").getModifiers() SHALL be Modifier.PRIVATE.
 *
 * On unfixed code → FAIL (modifier == PUBLIC).
 */
class TokenBug19ExplorationTest {

    @Test
    @DisplayName("BUG-19: Token.user field SHALL be private (not public)")
    void tokenUserFieldShallBePrivate() throws NoSuchFieldException {
        Field userField = Token.class.getDeclaredField("user");
        int modifiers = userField.getModifiers();

        assertThat(Modifier.isPrivate(modifiers))
                .as("Token.user field must be private for proper encapsulation and Hibernate lazy loading. " +
                    "Current modifier: %s", Modifier.toString(modifiers))
                .isTrue();
    }
}
