package com.example.shopnow.user;

import com.example.shopnow.user.models.Cart;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Exploration test for BUG-21: Missing CartProduct entity.
 *
 * Validates: Requirements 2.21
 *
 * Property 21: CartProduct entity tồn tại.
 * - Class.forName("com.example.shopnow.user.models.CartProduct") SHALL succeed
 *   AND class annotated @Entity.
 * - Cart class SHALL có field "items" @OneToMany(mappedBy="cart").
 *
 * On unfixed code → FAIL (ClassNotFoundException for CartProduct).
 */
class CartProductBug21ExplorationTest {

    @Test
    @DisplayName("BUG-21: CartProduct entity SHALL exist and be annotated with @Entity")
    void cartProductEntityShallExist() {
        Class<?> cartProductClass;
        try {
            cartProductClass = Class.forName("com.example.shopnow.user.models.CartProduct");
        } catch (ClassNotFoundException e) {
            assertThat(false)
                    .as("CartProduct entity class must exist at com.example.shopnow.user.models.CartProduct. " +
                        "Got ClassNotFoundException: %s", e.getMessage())
                    .isTrue();
            return; // unreachable, but satisfies compiler
        }

        Entity entityAnnotation = cartProductClass.getAnnotation(Entity.class);
        assertThat(entityAnnotation)
                .as("CartProduct class must be annotated with @Entity")
                .isNotNull();
    }

    @Test
    @DisplayName("BUG-21: Cart SHALL have field 'items' with @OneToMany(mappedBy=\"cart\")")
    void cartShallHaveItemsFieldWithOneToMany() throws NoSuchFieldException {
        Field itemsField;
        try {
            itemsField = Cart.class.getDeclaredField("items");
        } catch (NoSuchFieldException e) {
            assertThat(false)
                    .as("Cart class must have a field named 'items'. " +
                        "Got NoSuchFieldException: %s", e.getMessage())
                    .isTrue();
            return; // unreachable
        }

        OneToMany oneToMany = itemsField.getAnnotation(OneToMany.class);
        assertThat(oneToMany)
                .as("Cart.items field must be annotated with @OneToMany")
                .isNotNull();

        assertThat(oneToMany.mappedBy())
                .as("@OneToMany on Cart.items must have mappedBy='cart'")
                .isEqualTo("cart");
    }
}
