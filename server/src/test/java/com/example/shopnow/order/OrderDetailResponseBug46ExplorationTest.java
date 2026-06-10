package com.example.shopnow.order;

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
import static org.assertj.core.api.Assertions.fail;

/**
 * Exploration test for BUG-46: dead code OrderDetailResponse.
 *
 * <p><b>Validates: Requirements 2.46</b>
 *
 * <p>Property 40: Bug Condition — Xóa OrderDetailResponse dead code.
 * Two-part check:
 * <ol>
 *   <li>Try {@code Class.forName("com.example.shopnow.order.rest.dto.OrderDetailResponse")}
 *       — if {@code ClassNotFoundException}, property satisfied (class deleted).</li>
 *   <li>If class exists, scan {@code src/main/java} for any file that references
 *       "OrderDetailResponse" (excluding the class definition file itself).
 *       If 0 references found, the class is dead code → FAIL.</li>
 * </ol>
 *
 * <p>On unfixed code → FAIL (class exists but has no usage in any mapper/controller).
 */
class OrderDetailResponseBug46ExplorationTest {

    private static final String CLASS_FQCN = "com.example.shopnow.order.rest.dto.OrderDetailResponse";
    private static final Path SRC_MAIN_JAVA = Paths.get("src", "main", "java");
    private static final Path DEFINITION_FILE = Paths.get("src", "main", "java",
            "com", "example", "shopnow", "order", "rest", "dto", "OrderDetailResponse.java");
    private static final String CLASS_SIMPLE_NAME = "OrderDetailResponse";

    @Test
    @DisplayName("BUG-46: OrderDetailResponse SHALL be deleted OR have at least 1 usage in mapper/controller")
    void orderDetailResponse_shallBeDeletedOrHaveUsage() throws IOException {
        // Part 1: Check if the class exists on the classpath
        boolean classExists;
        try {
            Class.forName(CLASS_FQCN);
            classExists = true;
        } catch (ClassNotFoundException e) {
            classExists = false;
        }

        if (!classExists) {
            // Class has been deleted — property satisfied
            return;
        }

        // Part 2: Class exists — scan src/main/java for usages (excluding the definition file)
        assertThat(SRC_MAIN_JAVA).as("src/main/java directory must exist").exists();

        List<String> usages = new ArrayList<>();

        Files.walkFileTree(SRC_MAIN_JAVA, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".java")) {
                    return FileVisitResult.CONTINUE;
                }

                // Skip the definition file itself
                if (file.toAbsolutePath().normalize().equals(DEFINITION_FILE.toAbsolutePath().normalize())) {
                    return FileVisitResult.CONTINUE;
                }

                String content = Files.readString(file);
                if (content.contains(CLASS_SIMPLE_NAME)) {
                    usages.add(SRC_MAIN_JAVA.relativize(file).toString());
                }

                return FileVisitResult.CONTINUE;
            }
        });

        assertThat(usages)
                .as("OrderDetailResponse exists on classpath but has NO usage in any " +
                    "mapper, controller, or service file under src/main/java (excluding its own " +
                    "definition). Dead code SHALL be deleted OR have at least 1 active usage.")
                .isNotEmpty();
    }
}
