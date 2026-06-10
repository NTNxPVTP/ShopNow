package com.example.shopnow.security;

import com.example.shopnow.security.models.Token;
import com.example.shopnow.security.models.TokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-JPA slice test for the {@code Token.userId} mapping after the
 * persistence-isolation refactor (Task 4.1): the former
 * {@code @ManyToOne User user} association was replaced by a scalar
 * {@code @Column(name = "user_id") @JdbcTypeCode(SqlTypes.UUID) UUID userId},
 * keeping the physical {@code user_id} column unchanged (no schema/DDL change).
 *
 * <p><b>Validates: Requirements 3.5, 11.4.</b>
 *
 * <p>Reuses the established H2 slice wiring from the product repository
 * exploration tests (embedded H2 via {@link AutoConfigureTestDatabase} +
 * {@code TestH2EnumFriendlyDialect} to keep {@code @JdbcTypeCode(NAMED_ENUM)}
 * H2-compatible). Structural refactor spec — no Property-Based Testing.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-google",
        "spring.security.oauth2.client.registration.github.client-id=test-github",
        "spring.security.oauth2.client.registration.github.client-secret=test-github",
        "spring.jpa.properties.hibernate.dialect=com.example.shopnow.product.TestH2EnumFriendlyDialect",
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
@DirtiesContext
class TokenPersistenceTest {

    @Autowired
    private TokenRepository tokenRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Token.userId round-trips to/from the user_id column")
    void userId_roundTripsThroughUserIdColumn() {
        UUID userId = UUID.randomUUID();
        Token token = Token.builder()
                .token("tok-abc")
                .tokenType(TokenType.ACCESS_TOKEN)
                .userId(userId)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
        entityManager.flush();
        entityManager.clear();

        Optional<Token> reloaded = tokenRepository.findByToken("tok-abc");

        assertThat(reloaded)
                .as("Saved token SHALL be retrievable after flush/clear")
                .isPresent();
        assertThat(reloaded.get().getUserId())
                .as("userId SHALL round-trip from the user_id column")
                .isEqualTo(userId);
    }

    @Test
    @DisplayName("findAllValidTokenByUser returns the saved non-revoked/non-expired token")
    void findAllValidTokenByUser_returnsSavedToken() {
        UUID userId = UUID.randomUUID();
        Token token = Token.builder()
                .token("tok-valid")
                .tokenType(TokenType.ACCESS_TOKEN)
                .userId(userId)
                .expired(false)
                .revoked(false)
                .build();
        tokenRepository.save(token);
        entityManager.flush();
        entityManager.clear();

        List<Token> valid = tokenRepository.findAllValidTokenByUser(userId);

        assertThat(valid)
                .as("Valid tokens for the userId SHALL include the saved token")
                .extracting(Token::getToken)
                .contains("tok-valid");
    }
}
