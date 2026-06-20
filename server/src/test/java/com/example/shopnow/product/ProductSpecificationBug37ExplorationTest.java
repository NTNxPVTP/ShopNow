package com.example.shopnow.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-37: keyword chứa % hoặc _ không được escape trong SQL LIKE.
 *
 * <p><b>Validates: Requirements 2.37</b>
 *
 * <p>Property 31: Bug Condition — Keyword wildcard escape.
 * Read ProductSpecification.java source and assert it contains escape logic for SQL LIKE
 * wildcards (%, _, \). Specifically, the hasNameLike method should escape these characters
 * before building the LIKE predicate.
 *
 * <p>Approach: Static source scan. Read ProductSpecification.java and assert the source contains
 * escape logic (e.g., {@code .replace("%", "\\%")} or {@code .replace("_", "\\_")} or
 * {@code cb.like(field, pattern, '\\')} escape char parameter).
 *
 * <p>On unfixed code → FAIL (no escape logic exists).
 */
class ProductSpecificationBug37ExplorationTest {

    private static final Path SOURCE_FILE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "product", "infrastructure", "persistence",
            "ProductSpecification.java");

    @Test
    @DisplayName("BUG-37: hasNameLike SHALL escape '%' wildcard character before LIKE predicate")
    void hasNameLike_shallEscapePercentWildcard() throws IOException {
        assertThat(SOURCE_FILE).as("ProductSpecification.java must exist").exists();

        String source = Files.readString(SOURCE_FILE);

        // The source must contain escape logic for '%' character.
        // Valid patterns: .replace("%", "\\%") or .replace("%", "\\\\%") or replaceAll with %
        boolean escapesPercent = source.contains(".replace(\"%\"")
                || source.contains(".replace('%'")
                || source.contains("replaceAll(\"%\"")
                || source.contains("replaceAll(\"\\\\%\"");

        assertThat(escapesPercent)
                .as("ProductSpecification.hasNameLike SHALL escape '%%' wildcard character. " +
                    "The source must contain .replace(\"%%\", ...) or equivalent escape logic " +
                    "to prevent SQL injection via LIKE wildcards.")
                .isTrue();
    }

    @Test
    @DisplayName("BUG-37: hasNameLike SHALL escape '_' wildcard character before LIKE predicate")
    void hasNameLike_shallEscapeUnderscoreWildcard() throws IOException {
        assertThat(SOURCE_FILE).as("ProductSpecification.java must exist").exists();

        String source = Files.readString(SOURCE_FILE);

        // The source must contain escape logic for '_' character.
        boolean escapesUnderscore = source.contains(".replace(\"_\"")
                || source.contains(".replace('_'")
                || source.contains("replaceAll(\"_\"")
                || source.contains("replaceAll(\"\\\\_\"");

        assertThat(escapesUnderscore)
                .as("ProductSpecification.hasNameLike SHALL escape '_' wildcard character. " +
                    "The source must contain .replace(\"_\", ...) or equivalent escape logic " +
                    "to prevent SQL injection via LIKE wildcards.")
                .isTrue();
    }

    @Test
    @DisplayName("BUG-37: hasNameLike SHALL use escape character parameter in cb.like()")
    void hasNameLike_shallUseEscapeCharParameter() throws IOException {
        assertThat(SOURCE_FILE).as("ProductSpecification.java must exist").exists();

        String source = Files.readString(SOURCE_FILE);

        // The cb.like() call should use the 3-argument form with an escape character,
        // OR the escape logic should be applied before the like call.
        // Valid patterns: cb.like(expr, pattern, '\\') or cb.like(expr, pattern, escapeChar)
        // We check for either the escape char in cb.like OR the presence of both replace calls
        // (which would mean the wildcards are escaped in the pattern string itself).
        boolean hasEscapeCharInLike = source.contains("cb.like(")
                && (source.contains("'\\\\'") || source.contains("'\\\\'"));

        boolean hasReplaceForBothWildcards = source.contains(".replace(\"%\"")
                && source.contains(".replace(\"_\"");

        boolean hasAdequateEscapeLogic = hasEscapeCharInLike || hasReplaceForBothWildcards;

        assertThat(hasAdequateEscapeLogic)
                .as("ProductSpecification.hasNameLike SHALL either use cb.like(field, pattern, escapeChar) " +
                    "with an escape character parameter, OR escape both '%%' and '_' via .replace() calls. " +
                    "Currently, the method passes unescaped user input directly to LIKE, " +
                    "allowing '%%' to match all rows and '_' to match any single character.")
                .isTrue();
    }
}
