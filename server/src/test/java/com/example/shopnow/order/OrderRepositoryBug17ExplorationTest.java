package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-17: collection fetch + Pageable → in-memory pagination.
 *
 * <p><b>Validates: Requirements 2.17</b>
 *
 * <p>Phase 1 bug-condition exploration test. EXPECTED TO FAIL on unfixed code.
 *
 * <p><b>Bug condition C(17):</b> {@code OrderRepository.findAll(Specification, Pageable)}
 * is annotated with {@code @EntityGraph(attributePaths = "subOrders")} which fetches
 * a collection eagerly. When Hibernate applies pagination to a query that fetches a
 * collection via EntityGraph/JOIN FETCH, it logs warning {@code HHH000104
 * "firstResult/maxResults specified with collection fetch; applying in memory"}
 * and pulls ALL rows into JVM memory before paginating — defeating DB-level LIMIT/OFFSET.
 *
 * <p><b>Property P(17):</b> The {@code findAll(Specification, Pageable)} method in
 * {@code OrderRepository} SHALL NOT have {@code @EntityGraph} that fetches the
 * {@code subOrders} collection. Pagination must happen at the DB level (SQL LIMIT/OFFSET).
 *
 * <p><b>Test approach:</b> Reflection-based static scan. Since {@code OrderRepository}
 * is package-private, this test resides in the same package and uses reflection to
 * inspect the {@code findAll} method's annotations. The test asserts that NO method
 * accepting both {@code Specification} and {@code Pageable} parameters has an
 * {@code @EntityGraph} annotation whose {@code attributePaths} include "subOrders".
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code OrderRepository.findAll(Specification, Pageable)} has
 * {@code @EntityGraph(attributePaths = "subOrders")} → triggers HHH000104 in-memory
 * pagination. Test FAILS because the problematic annotation IS present.
 */
class OrderRepositoryBug17ExplorationTest {

    /**
     * Asserts that no method in OrderRepository that accepts both Specification and
     * Pageable has an @EntityGraph fetching "subOrders" collection.
     *
     * <p>On unfixed code, this test MUST FAIL because the override of
     * {@code findAll(Specification, Pageable)} is annotated with
     * {@code @EntityGraph(attributePaths = "subOrders")}.
     */
    @Test
    @DisplayName("BUG-17: findAll(Specification, Pageable) must NOT have @EntityGraph fetching subOrders collection")
    void findAll_withPageable_mustNotHaveEntityGraphFetchingSubOrders() {
        Class<?> repoClass = OrderRepository.class;

        // Find all methods that accept both Specification and Pageable
        boolean foundProblematicAnnotation = false;
        String problematicMethodName = null;

        for (Method method : repoClass.getDeclaredMethods()) {
            Class<?>[] paramTypes = method.getParameterTypes();
            boolean hasSpecification = Arrays.stream(paramTypes)
                    .anyMatch(Specification.class::isAssignableFrom);
            boolean hasPageable = Arrays.stream(paramTypes)
                    .anyMatch(Pageable.class::isAssignableFrom);

            if (hasSpecification && hasPageable) {
                EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);
                if (entityGraph != null) {
                    String[] attributePaths = entityGraph.attributePaths();
                    for (String path : attributePaths) {
                        if (path.equals("subOrders") || path.startsWith("subOrders.")) {
                            foundProblematicAnnotation = true;
                            problematicMethodName = method.getName();
                            break;
                        }
                    }
                }
            }
        }

        assertThat(foundProblematicAnnotation)
                .as("OrderRepository method '%s' has @EntityGraph fetching 'subOrders' collection "
                        + "alongside Pageable parameter. This causes Hibernate HHH000104 warning "
                        + "and in-memory pagination. The @EntityGraph must be removed from paginated "
                        + "queries or subOrders must not be fetched eagerly with pagination.",
                        problematicMethodName)
                .isFalse();
    }
}
