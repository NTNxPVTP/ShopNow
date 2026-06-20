package com.example.shopnow.product;

import com.example.shopnow.product.domain.models.Shop;
import jakarta.persistence.OneToMany;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-20: Shop.products @OneToMany missing mappedBy attribute.
 *
 * Validates: Requirements 2.20
 *
 * Property 20: Shop.products mappedBy.
 * Reflection/annotation check: Shop.class.getDeclaredField("products") annotation @OneToMany
 * SHALL have mappedBy attribute set (e.g. "shop").
 *
 * On unfixed code → FAIL (mappedBy is empty/default, causing Hibernate to generate
 * a join table shop_products instead of using FK products.shop_id).
 */
class ShopBug20ExplorationTest {

    @Test
    @DisplayName("BUG-20: Shop.products @OneToMany SHALL have mappedBy set (no join table)")
    void shopProductsShallHaveMappedBy() throws NoSuchFieldException {
        Field productsField = Shop.class.getDeclaredField("products");

        OneToMany oneToMany = productsField.getAnnotation(OneToMany.class);
        assertThat(oneToMany)
                .as("Shop.products field must be annotated with @OneToMany")
                .isNotNull();

        String mappedBy = oneToMany.mappedBy();
        assertThat(mappedBy)
                .as("@OneToMany on Shop.products must have mappedBy set (e.g. 'shop') " +
                    "to avoid Hibernate generating a join table 'shop_products'. " +
                    "Current mappedBy value: '%s'", mappedBy)
                .isNotEmpty();
    }
}
