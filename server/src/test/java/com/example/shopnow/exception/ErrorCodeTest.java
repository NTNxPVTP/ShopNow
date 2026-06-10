package com.example.shopnow.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ErrorCode} catalog.
 *
 * <p>Pure enum inspection (no Spring context, no collaborators). Verifies catalog
 * integrity (every value carries non-null fields) and uniqueness of the {@code code}
 * values across the enum.
 *
 * <p>Validates: Requirements 6.2, 6.3
 */
class ErrorCodeTest {

    @DisplayName("Every ErrorCode value has non-null code, title, message, and status")
    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    void everyErrorCode_hasNonNullFields(ErrorCode errorCode) {
        // Assert
        assertThat(errorCode.getCode()).as("code").isNotNull();
        assertThat(errorCode.getTitle()).as("title").isNotNull();
        assertThat(errorCode.getMessage()).as("message").isNotNull();
        assertThat(errorCode.getStatus()).as("status").isNotNull();
    }

    @Test
    @DisplayName("ErrorCode code values are unique across all enum constants")
    void errorCodeValues_areUnique() {
        // Arrange
        ErrorCode[] values = ErrorCode.values();

        // Act
        Set<String> distinctCodes = Arrays.stream(values)
                .map(ErrorCode::getCode)
                .collect(Collectors.toSet());

        // Assert: the number of distinct codes equals the number of enum constants
        assertThat(distinctCodes).hasSize(values.length);
    }
}
