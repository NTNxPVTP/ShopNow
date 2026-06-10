package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-48: Set non-deterministic order.
 *
 * <p><b>Validates: Requirements 2.48</b>
 *
 * <p>Property 42: Bug Condition — Deterministic subOrders order.
 * The Order entity uses {@code Set<SubOrder>} (HashSet) which has non-deterministic
 * iteration order. The mapper should impose a deterministic sort (e.g., by createdAt ASC)
 * when mapping to the DTO's {@code List<SubOrderDTO>}.
 *
 * <p>Approach: Check that either:
 * <ul>
 *   <li>Order.subOrders is a List (deterministic), OR</li>
 *   <li>The mapper applies sorting when converting Set to List (contains {@code .sorted(},
 *       {@code Comparator}, {@code .sort(}, {@code stream().sorted(} in the mapping logic)</li>
 * </ul>
 *
 * <p>On unfixed code → FAIL (Set is used and mapper doesn't sort).
 */
class SubOrderDeterministicBug48ExplorationTest {

    private static final Path ORDER_ENTITY_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "models", "Order.java");

    private static final Path ORDER_MAPPER_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "mapper", "OrderMapper.java");

    private static final Path SUB_ORDER_MAPPER_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "mapper", "SubOrderMapper.java");

    @Test
    @DisplayName("BUG-48: subOrders mapping SHALL impose deterministic order (sort by createdAt ASC)")
    void subOrdersMapping_shallImposeDeterministicOrder() throws IOException {
        assertThat(ORDER_ENTITY_SOURCE)
                .as("Order.java must exist")
                .exists();
        assertThat(ORDER_MAPPER_SOURCE)
                .as("OrderMapper.java must exist")
                .exists();
        assertThat(SUB_ORDER_MAPPER_SOURCE)
                .as("SubOrderMapper.java must exist")
                .exists();

        String orderSource = Files.readString(ORDER_ENTITY_SOURCE);
        String orderMapperSource = Files.readString(ORDER_MAPPER_SOURCE);
        String subOrderMapperSource = Files.readString(SUB_ORDER_MAPPER_SOURCE);

        // Check condition 1: Does Order.subOrders use a List (deterministic)?
        boolean usesListForSubOrders = orderSource.contains("List<SubOrder> subOrders")
                || orderSource.contains("LinkedHashSet<SubOrder> subOrders");

        // Check condition 2: Does the mapper apply sorting when mapping subOrders?
        // Look for sorting indicators in both OrderMapper and SubOrderMapper
        String allMapperSource = orderMapperSource + "\n" + subOrderMapperSource;
        boolean mapperAppliesSorting = allMapperSource.contains(".sorted(")
                || allMapperSource.contains("Comparator")
                || allMapperSource.contains(".sort(")
                || allMapperSource.contains("stream().sorted")
                || allMapperSource.contains("Collections.sort")
                || allMapperSource.contains("Sort.by")
                || allMapperSource.contains("createdAt")  // sorting by createdAt
                || allMapperSource.contains("@SortDefault");

        // The bug is confirmed if:
        // - Order uses Set<SubOrder> (non-deterministic) AND
        // - The mapper does NOT apply sorting
        // At least one of these conditions must be false for the code to be correct:
        // Either use a List (deterministic) OR apply sorting in the mapper.
        assertThat(usesListForSubOrders || mapperAppliesSorting)
                .as("BUG-48: Order.subOrders uses Set<SubOrder> (non-deterministic HashSet iteration order) "
                    + "AND the mapper does NOT impose a deterministic sort when mapping to List<SubOrderDTO>. "
                    + "Either Order.subOrders should be a List/LinkedHashSet, "
                    + "OR the mapper must sort subOrders (e.g., by createdAt ASC) before converting to List. "
                    + "Current state: usesListForSubOrders=%s, mapperAppliesSorting=%s",
                    usesListForSubOrders, mapperAppliesSorting)
                .isTrue();
    }
}
