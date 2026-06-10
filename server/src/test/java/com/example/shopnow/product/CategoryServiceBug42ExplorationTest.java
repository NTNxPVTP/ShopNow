package com.example.shopnow.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-42: sort categories in-memory instead of at DB level.
 *
 * <p><b>Validates: Requirements 2.42</b>
 *
 * <p>Property 36: Bug Condition — Sort categories at DB.
 * The {@code CategoryService.getCategories()} method should delegate sorting to the
 * database via {@code findAll(Sort.by("name"))} rather than calling {@code findAll()}
 * and then sorting in-memory using {@code Comparator}, {@code .sorted(}, or
 * {@code Collections.sort}.
 *
 * <p>Approach: Read CategoryService.java source, extract the {@code getCategories}
 * method body, and assert:
 * <ul>
 *   <li>The method does NOT contain in-memory sort patterns ({@code .sorted(},
 *       {@code Comparator}, {@code Collections.sort})</li>
 *   <li>The method DOES contain DB-level sort ({@code Sort.by} or {@code findAll(Sort})</li>
 * </ul>
 *
 * <p>On unfixed code → FAIL (uses {@code findAll()} then {@code .sorted(Comparator...)}).
 */
class CategoryServiceBug42ExplorationTest {

    private static final Path CATEGORY_SERVICE_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "product", "CategoryService.java");

    @Test
    @DisplayName("BUG-42: getCategories SHALL sort at DB level via findAll(Sort), not in-memory")
    void getCategories_shallSortAtDbLevel_notInMemory() throws IOException {
        assertThat(CATEGORY_SERVICE_SOURCE)
                .as("CategoryService.java must exist for static scan")
                .exists();

        String source = Files.readString(CATEGORY_SERVICE_SOURCE);

        // Extract the getCategories method body
        String methodName = "getCategories";
        int methodStart = source.indexOf("public List<CategoryResponse> getCategories");
        if (methodStart == -1) {
            methodStart = source.indexOf("List<CategoryResponse> getCategories");
        }
        assertThat(methodStart)
                .as("Method getCategories must exist in CategoryService.java")
                .isGreaterThanOrEqualTo(0);

        // Find the opening brace of the method body
        int bodyStart = source.indexOf('{', methodStart);
        assertThat(bodyStart)
                .as("Method getCategories must have a body (not abstract)")
                .isGreaterThan(methodStart);

        // Find the matching closing brace
        String methodBody = extractMethodBody(source, bodyStart);
        assertThat(methodBody)
                .as("Could not extract getCategories method body")
                .isNotEmpty();

        // Assert: the method body SHALL NOT contain in-memory sort patterns.
        // In-memory sorting indicators:
        // - .sorted(
        // - Comparator
        // - Collections.sort
        boolean hasInMemorySort = methodBody.contains(".sorted(")
                || methodBody.contains("Comparator")
                || methodBody.contains("Collections.sort");

        assertThat(hasInMemorySort)
                .as("CategoryService.getCategories SHALL NOT sort in-memory. "
                    + "The method must use findAll(Sort.by(\"name\")) to delegate sorting to the DB. "
                    + "Found in-memory sorting patterns in the method body:\n%s", methodBody)
                .isFalse();

        // Additionally assert: the method body SHALL contain DB-level sort usage
        boolean hasDbSort = methodBody.contains("Sort.by")
                || methodBody.contains("findAll(Sort")
                || methodBody.contains("Sort.by(\"name\")")
                || source.contains("import org.springframework.data.domain.Sort");

        assertThat(hasDbSort)
                .as("CategoryService.getCategories SHALL use Sort.by(\"name\") for DB-level sorting. "
                    + "No Sort usage found in the method body:\n%s", methodBody)
                .isTrue();
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
