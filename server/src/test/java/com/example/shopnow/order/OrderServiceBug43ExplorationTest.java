package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-43: HashSet with entity id null causes hash collision.
 *
 * <p><b>Validates: Requirements 2.43</b>
 *
 * <p>Property 37: Bug Condition — List/LinkedHashSet thay HashSet.
 * Static source scan of {@code OrderService.java}: extract the {@code createOrder} method body
 * and assert it does NOT contain {@code new HashSet<>()} or {@code HashSet<SubOrder>} or
 * {@code HashSet<OrderDetail>}.
 *
 * <p>Root cause: BaseEntity has {@code @EqualsAndHashCode(of = "id")} and OrderService.createOrder
 * uses {@code HashSet<SubOrder>} / {@code HashSet<OrderDetail>} and adds elements before persist
 * (id is null). Since all entities have id=null before persist, their hashCode is the same,
 * causing hash collisions that collapse multiple distinct entities into one entry in the HashSet.
 *
 * <p>On unfixed code → FAIL (HashSet is used, which causes hash collision when id is null before persist).
 */
class OrderServiceBug43ExplorationTest {

    private static final Path ORDER_SERVICE_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "OrderService.java");

    @Test
    @DisplayName("BUG-43: createOrder SHALL NOT use HashSet for SubOrder/OrderDetail collections (hash collision when id=null)")
    void createOrder_shallNotUseHashSet_forEntityCollections() throws IOException {
        assertThat(ORDER_SERVICE_SOURCE)
                .as("OrderService.java must exist for static scan")
                .exists();

        String source = Files.readString(ORDER_SERVICE_SOURCE);

        // Extract the createOrder method body
        int methodStart = source.indexOf("public OrderDTO createOrder(");
        if (methodStart == -1) {
            methodStart = source.indexOf("OrderDTO createOrder(");
        }
        assertThat(methodStart)
                .as("Method createOrder must exist in OrderService.java")
                .isGreaterThanOrEqualTo(0);

        // Find the opening brace of the method body
        int bodyStart = source.indexOf('{', methodStart);
        assertThat(bodyStart)
                .as("Method createOrder must have a body")
                .isGreaterThan(methodStart);

        // Find the matching closing brace
        String methodBody = extractMethodBody(source, bodyStart);
        assertThat(methodBody)
                .as("Could not extract createOrder method body")
                .isNotEmpty();

        // Assert: the method body SHALL NOT contain HashSet usage for entity collections.
        // Patterns that indicate the bug:
        // - new HashSet<>()
        // - new HashSet<SubOrder>
        // - new HashSet<OrderDetail>
        // - HashSet<SubOrder>
        // - HashSet<OrderDetail>
        boolean usesHashSetForEntities = methodBody.contains("new HashSet<>()")
                || methodBody.contains("new HashSet<SubOrder>")
                || methodBody.contains("new HashSet<OrderDetail>")
                || methodBody.contains("HashSet<SubOrder>")
                || methodBody.contains("HashSet<OrderDetail>");

        assertThat(usesHashSetForEntities)
                .as("OrderService.createOrder SHALL NOT use HashSet for SubOrder/OrderDetail collections. " +
                    "When entities have @EqualsAndHashCode(of=\"id\") and id is null before persist, " +
                    "all entities hash to the same bucket causing collisions that silently drop elements. " +
                    "Use List or LinkedHashSet instead. " +
                    "Found HashSet usage in createOrder method body:\n%s", methodBody)
                .isFalse();
    }

    /**
     * Extracts a method body starting from the opening brace, handling nested braces.
     */
    private String extractMethodBody(String source, int openBraceIndex) {
        int depth = 0;
        int end = openBraceIndex;
        for (int i = openBraceIndex; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            }
        }
        if (end > openBraceIndex) {
            return source.substring(openBraceIndex, end + 1);
        }
        return "";
    }
}
