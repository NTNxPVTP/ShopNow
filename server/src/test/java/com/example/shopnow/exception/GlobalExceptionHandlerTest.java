package com.example.shopnow.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Pure unit test (no Spring context, no collaborators). The handler is a pure
 * function of the {@link ErrorCode} carried by a {@link DomainException}: it builds a
 * {@link ResponseEntity} wrapping a {@link ProblemDetail} whose status, title, detail,
 * {@code errorCode} property, and {@code timestamp} property are derived from the code.
 *
 * <p>Validates: Requirements 6.1
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @DisplayName("handleDomainException maps each ErrorCode to a matching ProblemDetail response")
    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorCode.class)
    void handleDomainException_buildsProblemDetailFromErrorCode(ErrorCode errorCode) {
        // Arrange
        DomainException exception = new DomainException(errorCode);

        // Act
        ResponseEntity<ProblemDetail> response = handler.handleDomainException(exception);

        // Assert: HTTP status equals the error code's status
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(errorCode.getStatus());

        ProblemDetail problemDetail = response.getBody();
        assertThat(problemDetail).isNotNull();

        // Title and detail come straight from the error code
        assertThat(problemDetail.getTitle()).isEqualTo(errorCode.getTitle());
        assertThat(problemDetail.getDetail()).isEqualTo(errorCode.getMessage());

        // The numeric status on the ProblemDetail also reflects the error code
        assertThat(problemDetail.getStatus()).isEqualTo(errorCode.getStatus().value());

        // Custom properties: errorCode equals the code, timestamp present and an Instant
        Map<String, Object> properties = problemDetail.getProperties();
        assertThat(properties).isNotNull();
        assertThat(properties).containsEntry("errorCode", errorCode.getCode());
        assertThat(properties.get("timestamp"))
                .as("timestamp property")
                .isNotNull()
                .isInstanceOf(Instant.class);
    }
}
