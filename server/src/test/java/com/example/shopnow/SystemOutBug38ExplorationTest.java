package com.example.shopnow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-38: System.out.println in production code.
 *
 * <p><b>Validates: Requirements 2.38</b>
 *
 * <p>Property 32: Bug Condition - No System.out.println in src/main.
 * Static scan: walk src/main/java, grep System.out.println.
 * Assert: 0 occurrences.
 *
 * <p>On unfixed code this test FAILS (15+ occurrences across JwtFilter, AuthenticationService,
 * OrderService, CategoryController, SubOrderController, ShopnowApplication).
 */
class SystemOutBug38ExplorationTest {

    private static final Path SRC_MAIN_JAVA = Paths.get("src", "main", "java");
    private static final String SYSTEM_OUT_PRINTLN = "System.out.println";

    @Test
    @DisplayName("BUG-38: Production code SHALL NOT contain System.out.println")
    void productionCode_shallNotContain_systemOutPrintln() throws IOException {
        assertThat(SRC_MAIN_JAVA).as("src/main/java directory must exist").exists();

        List<String> violations = new ArrayList<>();

        Files.walkFileTree(SRC_MAIN_JAVA, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.toString().endsWith(".java")) {
                    List<String> lines = Files.readAllLines(file);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        // Skip lines that are comments (single-line // or inside block comments)
                        String trimmed = line.trim();
                        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                            continue;
                        }
                        if (line.contains(SYSTEM_OUT_PRINTLN)) {
                            violations.add(String.format("%s:%d → %s",
                                    SRC_MAIN_JAVA.relativize(file), i + 1, trimmed));
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertThat(violations)
                .as("Production code (src/main/java/**/*.java) SHALL have 0 occurrences of " +
                    "System.out.println. Found %d occurrences:\n%s",
                    violations.size(), String.join("\n", violations))
                .isEmpty();
    }
}
