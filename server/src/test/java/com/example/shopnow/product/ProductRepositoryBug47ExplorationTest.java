package com.example.shopnow.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Exploration test for BUG-47: typo findWithPageReponseBy in ProductRepository.
 *
 * <p><b>Validates: Requirements 2.47</b>
 *
 * <p>Property 41: Bug Condition — Xóa typo findWithPageReponseBy.
 * Reflection: {@code ProductRepository.class.getMethod("findWithPageReponseBy", Pageable.class)}
 * SHALL throw {@code NoSuchMethodException} (i.e., the typo method should NOT exist).
 *
 * <p>On unfixed code → FAIL (method exists because the typo has not been removed).
 */
class ProductRepositoryBug47ExplorationTest {

    @Test
    @DisplayName("BUG-47: ProductRepository SHALL NOT have typo method findWithPageReponseBy")
    void findWithPageReponseBy_shallNotExist() {
        try {
            // Attempt to find the typo method via reflection
            ProductRepository.class.getMethod("findWithPageReponseBy", Pageable.class);

            // If we reach here, the method exists — this is the bug condition
            fail("ProductRepository still contains the typo method 'findWithPageReponseBy(Pageable)'. " +
                 "This method should be removed (typo: 'Reponse' instead of 'Response'). " +
                 "Bug condition confirmed: method exists with no callers.");
        } catch (NoSuchMethodException e) {
            // Expected: method does not exist — property satisfied (bug is fixed)
            assertThat(e).isNotNull();
        }
    }
}
