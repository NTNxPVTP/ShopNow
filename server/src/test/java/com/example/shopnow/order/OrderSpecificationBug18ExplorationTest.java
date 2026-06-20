package com.example.shopnow.order;

import com.example.shopnow.order.infrastructure.persistence.OrderSpecification;
import com.example.shopnow.product.infrastructure.persistence.ProductSpecification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-18: count query nhân join.
 *
 * <p><b>Validates: Requirements 2.18</b>
 *
 * <p>Phase 1 bug-condition exploration test. EXPECTED TO FAIL on unfixed code.
 *
 * <p><b>Bug condition C(18):</b> When {@code OrderSpecification.hasShopId} or
 * {@code ProductSpecification.hasCategoryId} is used with paginated queries,
 * the Specification's {@code toPredicate} always performs a join (e.g.
 * {@code root.join("subOrders")} / {@code root.join("categories")}) even for
 * count queries where {@code query.getResultType() == Long.class}. This causes
 * the count query to multiply rows via the join, making
 * {@code Page.totalElements} larger than the actual DISTINCT entity count.
 *
 * <p><b>Property P(18):</b> The Specification SHALL skip the join when
 * {@code query.getResultType() == Long.class} (count query), returning
 * {@code cb.conjunction()} instead. This ensures {@code Page.totalElements}
 * equals the actual number of DISTINCT entities.
 *
 * <p><b>Test approach:</b> Static source scan. Read the source files of
 * {@code OrderSpecification.java} and {@code ProductSpecification.java} and
 * assert that the {@code hasShopId} / {@code hasCategoryId} methods contain
 * a guard checking {@code query.getResultType()} (or {@code Long.class})
 * before performing the join. On unfixed code, no such guard exists — the
 * join always happens — so the test FAILS.
 *
 * <p><b>Expected counterexample on unfixed code:</b>
 * {@code OrderSpecification.hasShopId} performs {@code root.join("subOrders")}
 * unconditionally. {@code ProductSpecification.hasCategoryId} performs
 * {@code root.join("categories")} unconditionally. Neither checks
 * {@code query.getResultType()} → count query multiplies rows.
 */
class OrderSpecificationBug18ExplorationTest {

    private static final Path ORDER_SPEC_PATH = Path.of(
            "src/main/java/com/example/shopnow/order/infrastructure/persistence/OrderSpecification.java");
    private static final Path PRODUCT_SPEC_PATH = Path.of(
            "src/main/java/com/example/shopnow/product/infrastructure/persistence/ProductSpecification.java");

    /**
     * Asserts that OrderSpecification.hasShopId checks query.getResultType()
     * before performing the join on subOrders.
     *
     * <p>On unfixed code, this test MUST FAIL because hasShopId always joins
     * without checking if it's a count query.
     */
    @Test
    @DisplayName("BUG-18: OrderSpecification.hasShopId must guard join with query.getResultType() check")
    void orderSpecification_hasShopId_mustGuardJoinForCountQuery() throws IOException {
        String source = Files.readString(ORDER_SPEC_PATH);

        // Extract the hasShopId method body
        String methodBody = extractMethodBody(source, "hasShopId");
        assertThat(methodBody)
                .as("Could not find hasShopId method in OrderSpecification.java")
                .isNotNull();

        // The fix requires checking query.getResultType() before joining.
        // This prevents the count query from multiplying rows via the join.
        boolean hasResultTypeCheck = methodBody.contains("getResultType()")
                || methodBody.contains("resultType")
                || methodBody.contains("Long.class");

        assertThat(hasResultTypeCheck)
                .as("OrderSpecification.hasShopId performs root.join(\"subOrders\") "
                        + "unconditionally without checking query.getResultType(). "
                        + "This causes count queries to multiply rows via the join, "
                        + "making Page.totalElements = N * joinFactor instead of N. "
                        + "The method must check if query.getResultType() == Long.class "
                        + "and skip the join for count queries.")
                .isTrue();
    }

    /**
     * Asserts that ProductSpecification.hasCategoryId checks query.getResultType()
     * before performing the join on categories.
     *
     * <p>On unfixed code, this test MUST FAIL because hasCategoryId always joins
     * without checking if it's a count query.
     */
    @Test
    @DisplayName("BUG-18: ProductSpecification.hasCategoryId must guard join with query.getResultType() check")
    void productSpecification_hasCategoryId_mustGuardJoinForCountQuery() throws IOException {
        String source = Files.readString(PRODUCT_SPEC_PATH);

        // Extract the hasCategoryId method body
        String methodBody = extractMethodBody(source, "hasCategoryId");
        assertThat(methodBody)
                .as("Could not find hasCategoryId method in ProductSpecification.java")
                .isNotNull();

        // The fix requires checking query.getResultType() before joining.
        boolean hasResultTypeCheck = methodBody.contains("getResultType()")
                || methodBody.contains("resultType")
                || methodBody.contains("Long.class");

        assertThat(hasResultTypeCheck)
                .as("ProductSpecification.hasCategoryId performs root.join(\"categories\") "
                        + "unconditionally without checking query.getResultType(). "
                        + "This causes count queries to multiply rows via the join, "
                        + "making Page.totalElements = N * joinFactor instead of N. "
                        + "The method must check if query.getResultType() == Long.class "
                        + "and skip the join for count queries.")
                .isTrue();
    }

    /**
     * Extracts the body of a method from Java source code.
     * Returns the text from the method signature through its closing brace.
     */
    private String extractMethodBody(String source, String methodName) {
        // Find the method declaration
        int methodStart = source.indexOf("public static Specification");
        while (methodStart != -1) {
            int nameStart = source.indexOf(methodName, methodStart);
            if (nameStart != -1 && nameStart < methodStart + 200) {
                // Found the right method - extract until matching closing brace
                int braceStart = source.indexOf("{", nameStart);
                if (braceStart == -1) return null;

                int depth = 0;
                int i = braceStart;
                while (i < source.length()) {
                    if (source.charAt(i) == '{') depth++;
                    else if (source.charAt(i) == '}') {
                        depth--;
                        if (depth == 0) {
                            return source.substring(nameStart, i + 1);
                        }
                    }
                    i++;
                }
                return null;
            }
            methodStart = source.indexOf("public static Specification", methodStart + 1);
        }
        return null;
    }
}
