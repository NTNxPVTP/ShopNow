package com.example.shopnow.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-39: jjwt deprecated API (defer).
 *
 * <p><b>Validates: Requirements 2.39</b>
 *
 * <p>Property 33: Bug Condition - jjwt deprecated comment.
 * Static scan: read JwtService.java; assert it contains a comment warning about
 * deprecated jjwt API usage (e.g., "// DEPRECATED:" or "// TODO: upgrade jjwt 0.12+").
 *
 * <p>On unfixed code this test FAILS because JwtService.java has no such comment
 * warning about the deprecated API usage that will break on jjwt 0.12+ upgrade.
 */
class JwtServiceBug39ExplorationTest {

    private static final Path JWT_SERVICE_PATH = Paths.get(
            "src", "main", "java", "com", "example", "shopnow", "security", "JwtService.java");

    @Test
    @DisplayName("BUG-39: JwtService SHALL contain comment warning about deprecated jjwt API")
    void jwtService_shallContain_deprecatedApiWarningComment() throws IOException {
        assertThat(JWT_SERVICE_PATH)
                .as("JwtService.java must exist at expected path")
                .exists();

        String content = Files.readString(JWT_SERVICE_PATH);
        List<String> lines = Files.readAllLines(JWT_SERVICE_PATH);

        // Look for any comment that warns about deprecated jjwt API usage.
        // Acceptable patterns:
        //   "// DEPRECATED:" (general deprecation marker)
        //   "// TODO: upgrade jjwt 0.12+" (specific upgrade TODO)
        //   "DEPRECATED" in a comment context referencing jjwt
        //   "deprecated" referencing jjwt/parserBuilder/setClaims etc.
        boolean hasDeprecatedComment = false;

        for (String line : lines) {
            String trimmed = line.trim();
            // Check single-line comments and block comment lines
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                String upper = trimmed.toUpperCase();
                if (upper.contains("DEPRECATED") || upper.contains("TODO: UPGRADE JJWT")
                        || upper.contains("TODO: UPGRADE JJWT 0.12")
                        || upper.contains("JJWT 0.12")) {
                    hasDeprecatedComment = true;
                    break;
                }
            }
        }

        assertThat(hasDeprecatedComment)
                .as("JwtService.java SHALL contain a comment warning about deprecated jjwt API "
                    + "(e.g., '// DEPRECATED: ...' or '// TODO: upgrade jjwt 0.12+'). "
                    + "This documents that the current API (Jwts.parserBuilder(), setClaims(), "
                    + "setSubject(), signWith(key, SignatureAlgorithm)) will break on jjwt 0.12+ "
                    + "and the upgrade is deferred.")
                .isTrue();
    }
}
