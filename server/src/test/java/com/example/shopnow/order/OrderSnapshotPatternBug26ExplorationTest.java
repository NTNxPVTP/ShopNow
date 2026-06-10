package com.example.shopnow.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-26: Order/OrderDetail snapshot pattern not documented.
 *
 * Validates: Requirements 2.26
 *
 * Property 26: Order/OrderDetail snapshot pattern documented.
 * Static check: parse Javadoc of Order.customerId and OrderDetail.productId;
 * assert contains keyword "snapshot" (or equivalent like "snapshot pattern", "denormalized snapshot").
 *
 * On unfixed code → FAIL (Javadoc is empty / does not explain the snapshot pattern).
 */
class OrderSnapshotPatternBug26ExplorationTest {

    private static final Path ORDER_SOURCE = Paths.get("src/main/java/com/example/shopnow/order/models/Order.java");
    private static final Path ORDER_DETAIL_SOURCE = Paths.get("src/main/java/com/example/shopnow/order/models/OrderDetail.java");

    /**
     * Regex to find a Javadoc comment (/** ... * /) immediately preceding a line that declares customerId or productId.
     * We look for the keyword "snapshot" (case-insensitive) within that Javadoc block.
     */
    private static final Pattern JAVADOC_BEFORE_FIELD = Pattern.compile(
            "/\\*\\*([^*]|\\*(?!/))*\\*/\\s*\\n\\s*.*\\b%s\\b",
            Pattern.DOTALL
    );

    private static final Pattern SNAPSHOT_KEYWORD = Pattern.compile(
            "(?i)snapshot",
            Pattern.DOTALL
    );

    @Test
    @DisplayName("BUG-26: Order.customerId SHALL have Javadoc documenting snapshot pattern")
    void orderCustomerIdShallDocumentSnapshotPattern() throws IOException {
        String source = Files.readString(ORDER_SOURCE);

        // Extract Javadoc comment block immediately before the customerId field declaration
        String javadoc = extractJavadocBeforeField(source, "customerId");

        assertThat(javadoc)
                .as("Order.customerId must have a Javadoc comment explaining the snapshot pattern. " +
                    "The field stores a denormalized copy of the customer ID at order time.")
                .isNotNull();

        assertThat(javadoc)
                .as("Order.customerId Javadoc must contain the keyword 'snapshot' (or equivalent) " +
                    "to document the snapshot/denormalization pattern")
                .matches(s -> SNAPSHOT_KEYWORD.matcher(s).find());
    }

    @Test
    @DisplayName("BUG-26: OrderDetail.productId SHALL have Javadoc documenting snapshot pattern")
    void orderDetailProductIdShallDocumentSnapshotPattern() throws IOException {
        String source = Files.readString(ORDER_DETAIL_SOURCE);

        // Extract Javadoc comment block immediately before the productId field declaration
        String javadoc = extractJavadocBeforeField(source, "productId");

        assertThat(javadoc)
                .as("OrderDetail.productId must have a Javadoc comment explaining the snapshot pattern. " +
                    "The field stores a denormalized copy of the product ID at order time.")
                .isNotNull();

        assertThat(javadoc)
                .as("OrderDetail.productId Javadoc must contain the keyword 'snapshot' (or equivalent) " +
                    "to document the snapshot/denormalization pattern")
                .matches(s -> SNAPSHOT_KEYWORD.matcher(s).find());
    }

    /**
     * Extracts the Javadoc comment block (if any) that immediately precedes the given field name.
     * Returns null if no Javadoc is found before the field.
     */
    private String extractJavadocBeforeField(String source, String fieldName) {
        // Pattern: find a Javadoc block followed by optional annotations/whitespace, then the field declaration
        Pattern pattern = Pattern.compile(
                "(/\\*\\*(?:[^*]|\\*(?!/))*\\*/)\\s*(?:@\\w+(?:\\([^)]*\\))?\\s*)*.*\\b" + Pattern.quote(fieldName) + "\\b",
                Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
