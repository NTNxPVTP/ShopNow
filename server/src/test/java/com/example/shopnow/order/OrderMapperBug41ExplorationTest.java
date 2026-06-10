package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-41: duplicate PageResponse build in OrderMapper.
 *
 * <p><b>Validates: Requirements 2.41</b>
 *
 * <p>Property 35: Bug Condition — Mapper delegate to GenericMapper.
 * Static scan {@code OrderMapper.java}: assert method {@code toSummaryPageResponse} body
 * only delegates to {@code GenericMapper.toPageResponse(...)} and does NOT manually build
 * {@code PageInfo} (no {@code PageInfo.builder()}, no {@code new PageInfo(}, no
 * {@code .pageNumber(}, {@code .pageSize(}, {@code .totalPages(}, etc.).
 *
 * <p>Approach: Read OrderMapper.java source, extract the {@code toSummaryPageResponse}
 * method body, and assert it does NOT contain manual PageInfo construction patterns.
 * The method should only delegate to {@code GenericMapper.toPageResponse} or an equivalent
 * single-call delegation.
 *
 * <p>On unfixed code → FAIL (manual PageInfo.builder() construction exists in the method).
 */
class OrderMapperBug41ExplorationTest {

    private static final Path ORDER_MAPPER_SOURCE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "mapper", "OrderMapper.java");

    @Test
    @DisplayName("BUG-41: toSummaryPageResponse SHALL delegate to GenericMapper.toPageResponse, not build PageInfo manually")
    void toSummaryPageResponse_shallDelegateToGenericMapper_notBuildPageInfoManually() throws IOException {
        assertThat(ORDER_MAPPER_SOURCE)
                .as("OrderMapper.java must exist for static scan")
                .exists();

        String source = Files.readString(ORDER_MAPPER_SOURCE);

        // Extract the toSummaryPageResponse method body.
        // Find the method signature and extract everything until the closing brace.
        String methodName = "toSummaryPageResponse";
        int methodStart = source.indexOf("default PageResponse<OrderSummaryDTO> toSummaryPageResponse");
        if (methodStart == -1) {
            // Try alternative signature patterns
            methodStart = source.indexOf("PageResponse<OrderSummaryDTO> toSummaryPageResponse");
        }
        assertThat(methodStart)
                .as("Method toSummaryPageResponse must exist in OrderMapper.java")
                .isGreaterThanOrEqualTo(0);

        // Find the opening brace of the method body
        int bodyStart = source.indexOf('{', methodStart);
        assertThat(bodyStart)
                .as("Method toSummaryPageResponse must have a body (not abstract)")
                .isGreaterThan(methodStart);

        // Find the matching closing brace
        String methodBody = extractMethodBody(source, bodyStart);
        assertThat(methodBody)
                .as("Could not extract toSummaryPageResponse method body")
                .isNotEmpty();

        // Assert: the method body SHALL NOT contain manual PageInfo construction.
        // Manual construction indicators:
        // - PageInfo.builder()
        // - new PageInfo(
        // - .pageNumber(
        // - .pageSize(
        // - .totalPages(
        // - .isLast(
        // - .totalElements(
        boolean hasManualPageInfoBuild = methodBody.contains("PageInfo.builder()")
                || methodBody.contains("new PageInfo(")
                || methodBody.contains(".pageNumber(")
                || methodBody.contains(".pageSize(")
                || methodBody.contains(".totalPages(")
                || methodBody.contains(".isLast(")
                || methodBody.contains(".totalElements(");

        assertThat(hasManualPageInfoBuild)
                .as("OrderMapper.toSummaryPageResponse SHALL NOT manually build PageInfo. " +
                    "The method must delegate to GenericMapper.toPageResponse(...) instead of " +
                    "duplicating the PageInfo construction logic. " +
                    "Found manual PageInfo building patterns in the method body: \n%s", methodBody)
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
