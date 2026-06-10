package com.example.shopnow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-45: verbose SQL log ở profile mặc định.
 *
 * <p><b>Validates: Requirements 2.45</b>
 *
 * <p>Property 39: Bug Condition - Production profile log gọn.
 * Read application.properties (non-dev profile): assert KHÔNG có key
 * spring.jpa.show-sql=true, format_sql=true,
 * logging.level.org.hibernate.orm.jdbc.bind=TRACE,
 * spring.jpa.properties.hibernate.generate_statistics=true.
 *
 * <p>On unfixed code this test FAILS because all 4 verbose logging properties
 * are enabled in the default application.properties.
 */
class VerboseSqlLogBug45ExplorationTest {

    private static final Path APPLICATION_PROPERTIES =
            Paths.get("src", "main", "resources", "application.properties");

    /**
     * Verbose SQL logging properties that MUST NOT be enabled in the default
     * (production) profile. These should only exist in application-dev.properties.
     */
    private static final List<VerboseProperty> FORBIDDEN_PROPERTIES = List.of(
            new VerboseProperty("spring.jpa.show-sql", "true"),
            new VerboseProperty("spring.jpa.properties.hibernate.format_sql", "true"),
            new VerboseProperty("logging.level.org.hibernate.orm.jdbc.bind", "trace"),
            new VerboseProperty("spring.jpa.properties.hibernate.generate_statistics", "true")
    );

    @Test
    @DisplayName("BUG-45: Default profile SHALL NOT have verbose SQL logging enabled")
    void defaultProfile_shallNot_haveVerboseSqlLogging() throws IOException {
        assertThat(APPLICATION_PROPERTIES)
                .as("application.properties must exist")
                .exists();

        // Read the raw content to check for properties (handles both key=value formats)
        String content = Files.readString(APPLICATION_PROPERTIES);
        List<String> lines = Files.readAllLines(APPLICATION_PROPERTIES);

        // Also load as Properties for structured check
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(APPLICATION_PROPERTIES)) {
            props.load(reader);
        }

        List<String> violations = new ArrayList<>();

        for (VerboseProperty forbidden : FORBIDDEN_PROPERTIES) {
            String value = props.getProperty(forbidden.key());
            if (value != null && value.trim().equalsIgnoreCase(forbidden.forbiddenValue())) {
                violations.add(String.format("  %s=%s", forbidden.key(), value.trim()));
            }
        }

        assertThat(violations)
                .as("application.properties (default/production profile) SHALL NOT contain " +
                    "verbose SQL logging properties. Found %d violations:\n%s\n\n" +
                    "These properties should be moved to application-dev.properties.",
                    violations.size(), String.join("\n", violations))
                .isEmpty();
    }

    private record VerboseProperty(String key, String forbiddenValue) {}
}
