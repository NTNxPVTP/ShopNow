package com.example.shopnow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-34: Test files are commented out, so mvn test runs 0 tests.
 *
 * <p><b>Validates: Requirements 2.34</b>
 *
 * <p>Property 29: Bug Condition — Test suite chạy.
 * Static check: parse 2 file test source (ArchitectureTest.java, ShopnowApplicationTests.java),
 * assert KHÔNG có comment block bao quanh class declaration.
 *
 * <p>On unfixed code → FAIL (ArchitectureTest.java is entirely commented out with {@code //}).
 */
class TestSuiteBug34ExplorationTest {

    /**
     * Pattern matching an uncommented class declaration.
     * Matches lines like: "class ArchitectureTest {" or "public class ShopnowApplicationTests {"
     * Allows optional modifiers (public, abstract, etc.) before "class".
     */
    private static final Pattern UNCOMMENTED_CLASS_DECL = Pattern.compile(
            "^\\s*(?:public\\s+|abstract\\s+|final\\s+)*class\\s+\\w+");

    /**
     * Pattern detecting a commented-out class declaration.
     * Matches lines like: "// public class ArchitectureTest {" or "// class Foo {"
     */
    private static final Pattern COMMENTED_CLASS_DECL = Pattern.compile(
            "^\\s*//\\s*(?:public\\s+|abstract\\s+|final\\s+)*class\\s+\\w+");

    private static final Path TEST_SOURCE_ROOT = Paths.get("src", "test", "java",
            "com", "example", "shopnow");

    @Test
    @DisplayName("BUG-34: ArchitectureTest.java SHALL have an uncommented class declaration")
    void architectureTest_classDeclaration_shallNotBeCommented() throws IOException {
        Path file = TEST_SOURCE_ROOT.resolve("ArchitectureTest.java");
        assertThat(file).as("ArchitectureTest.java must exist").exists();

        List<String> lines = Files.readAllLines(file);

        boolean hasUncommentedClass = lines.stream()
                .anyMatch(line -> UNCOMMENTED_CLASS_DECL.matcher(line).find());
        boolean hasCommentedClass = lines.stream()
                .anyMatch(line -> COMMENTED_CLASS_DECL.matcher(line).find());

        assertThat(hasUncommentedClass)
                .as("ArchitectureTest.java SHALL contain an uncommented class declaration. " +
                    "Found commented class: %s", hasCommentedClass)
                .isTrue();
        assertThat(hasCommentedClass)
                .as("ArchitectureTest.java SHALL NOT have a commented-out class declaration")
                .isFalse();
    }

    @Test
    @DisplayName("BUG-34: ShopnowApplicationTests.java SHALL have an uncommented class declaration")
    void shopnowApplicationTests_classDeclaration_shallNotBeCommented() throws IOException {
        Path file = TEST_SOURCE_ROOT.resolve("ShopnowApplicationTests.java");
        assertThat(file).as("ShopnowApplicationTests.java must exist").exists();

        List<String> lines = Files.readAllLines(file);

        boolean hasUncommentedClass = lines.stream()
                .anyMatch(line -> UNCOMMENTED_CLASS_DECL.matcher(line).find());
        boolean hasCommentedClass = lines.stream()
                .anyMatch(line -> COMMENTED_CLASS_DECL.matcher(line).find());

        assertThat(hasUncommentedClass)
                .as("ShopnowApplicationTests.java SHALL contain an uncommented class declaration")
                .isTrue();
        assertThat(hasCommentedClass)
                .as("ShopnowApplicationTests.java SHALL NOT have a commented-out class declaration")
                .isFalse();
    }

    @Test
    @DisplayName("BUG-34: Neither test file SHALL be entirely commented out")
    void testFiles_shallNotBeEntirelyCommented() throws IOException {
        // Check ArchitectureTest.java - count non-empty, non-comment lines
        Path archFile = TEST_SOURCE_ROOT.resolve("ArchitectureTest.java");
        assertThat(archFile).as("ArchitectureTest.java must exist").exists();

        List<String> archLines = Files.readAllLines(archFile);
        long archActiveLines = archLines.stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.trim().startsWith("//"))
                .count();

        assertThat(archActiveLines)
                .as("ArchitectureTest.java SHALL have active (non-comment, non-blank) lines. " +
                    "Total lines: %d, all appear commented out.", archLines.size())
                .isGreaterThan(0);

        // Check ShopnowApplicationTests.java
        Path appTestFile = TEST_SOURCE_ROOT.resolve("ShopnowApplicationTests.java");
        List<String> appTestLines = Files.readAllLines(appTestFile);
        long appTestActiveLines = appTestLines.stream()
                .filter(line -> !line.isBlank())
                .filter(line -> !line.trim().startsWith("//"))
                .count();

        assertThat(appTestActiveLines)
                .as("ShopnowApplicationTests.java SHALL have active (non-comment, non-blank) lines")
                .isGreaterThan(0);
    }
}
