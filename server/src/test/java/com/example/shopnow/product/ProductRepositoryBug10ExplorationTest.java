package com.example.shopnow.product;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-10 ({@code ProductRepository.decreaseQuantity}
 * brings {@code Product.quantity} to {@code 0} but leaves
 * {@code status = ACTIVE}, so the product is never automatically marked
 * as {@code SOLD}).
 *
 * <p><b>Validates: Requirements 2.10</b> (Property 10 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug exists.
 * After the fix in {@link ProductRepository#decreaseQuantity} (extending
 * the JPQL UPDATE to flip {@code status} to {@code SOLD} when the
 * resulting quantity reaches zero), this test must turn green.
 *
 * <p><b>Bug condition C(10):</b> a row in {@code products} with
 * {@code status = ACTIVE}, {@code isDeleted = false} and
 * {@code quantity = qty > 0}, and a call
 * {@code decreaseQuantity(id, qty)} that subtracts the entire stock. The
 * production JPQL only mutates {@code quantity} (never touches
 * {@code status}), so the row ends up with {@code quantity = 0} and
 * {@code status = ACTIVE} — a sold-out product still advertised as
 * available.
 *
 * <p><b>Property P(10):</b> for any product {@code X} satisfying
 * {@code isBugCondition_10(X)},
 * {@link ProductRepository#decreaseQuantity(UUID, int)} SHALL leave the
 * persisted row with {@code quantity == 0} <i>and</i>
 * {@code status == SOLD} when reloaded.
 *
 * <p><b>Test design:</b> a Spring Data JPA slice ({@code @DataJpaTest})
 * with the embedded H2 datasource (replacing the production Postgres
 * {@code DB_URL}). Same setup as the BUG-9 sibling test
 * ({@link ProductRepositoryBug9ExplorationTest}) — only the entities
 * touched by the WHERE clause ({@link Product} + its mandatory
 * {@link Shop} relation) are involved, persistence is driven through
 * {@link EntityManager}, and the {@link TestH2EnumFriendlyDialect} keeps
 * the Postgres {@code @JdbcTypeCode(NAMED_ENUM)} mapping schema-compatible
 * with H2 by storing the enum literal as VARCHAR.
 *
 * <p>The test is parameterised over several initial stock sizes
 * ({@code 1}, {@code 5}, {@code 10}) to demonstrate the property holds
 * regardless of how much stock is being drained — the bug is about the
 * zero-crossing transition, not the magnitude.
 *
 * <p><b>Expected counterexample on unfixed code:</b> seed a product with
 * {@code status = ACTIVE}, {@code isDeleted = false}, {@code quantity =
 * qty}; call {@code productRepository.decreaseQuantity(id, qty)} → returns
 * {@code 1}, reloaded {@code quantity == 0} but reloaded
 * {@code status == ACTIVE} (assertion expects {@code SOLD}).
 */
@DataJpaTest
// Force H2 even though the project also has Postgres on the runtime
// classpath; a DataJpaTest slice would otherwise try to connect to the
// configured production DB_URL and fail outside the integration env.
@AutoConfigureTestDatabase(replace = Replace.ANY)
@TestPropertySource(properties = {
        // Hibernate creates schema for every Spring context (DDL is the
        // canonical source of truth for this slice test).
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // H2 only — turn off the production-only OAuth2 client autoconfig
        // so the slice doesn't need GG_OAUTH2ID_CLIENT etc. on the env.
        "spring.security.oauth2.client.registration.google.client-id=test-google",
        "spring.security.oauth2.client.registration.google.client-secret=test-google",
        "spring.security.oauth2.client.registration.github.client-id=test-github",
        "spring.security.oauth2.client.registration.github.client-secret=test-github",
        // The production JPA properties block depends on env vars only
        // for datasource; the slice replaces datasource via
        // @AutoConfigureTestDatabase, so the rest just need to stay
        // tolerant of H2.
        "spring.jpa.properties.hibernate.dialect=com.example.shopnow.product.TestH2EnumFriendlyDialect",
        // Quiet down the verbose default logging from the production
        // application.properties so a failing assertion surfaces clearly.
        "spring.jpa.show-sql=false",
        "logging.level.org.hibernate.orm.jdbc.bind=WARN"
})
@DirtiesContext
class ProductRepositoryBug10ExplorationTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest(name = "[{index}] initialQuantity={0} -> after decreaseQuantity(id, {0}) status SHALL be SOLD")
    @ValueSource(ints = {1, 5, 10})
    @DisplayName("decreaseQuantity SHALL flip status to SOLD when stock reaches 0")
    void decreaseQuantity_drainsToZero_shallFlipStatusToSold(int initialQuantity) {
        // --- Arrange --------------------------------------------------------
        Shop shop = new Shop();
        shop.setName("Bug10 Shop qty=" + initialQuantity);
        shop.setOwnerId(UUID.randomUUID());
        shop.setIsActive(true);
        entityManager.persist(shop);

        Product product = Product.builder()
                .shop(shop)
                .name("Bug10 Product qty=" + initialQuantity)
                .price(BigDecimal.valueOf(10))
                .quantity(initialQuantity)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();
        entityManager.persist(product);
        // Force INSERT before the JPQL UPDATE (which bypasses the
        // persistence context) so the row is visible to the update.
        entityManager.flush();
        UUID productId = product.getId();
        // Detach so the next read after the UPDATE pulls fresh DB state
        // rather than the in-memory snapshot.
        entityManager.clear();

        // --- Act ------------------------------------------------------------
        // Subtract the entire stock so the resulting quantity is exactly 0,
        // i.e. exercise the zero-crossing transition described in C(10).
        int rowsUpdated = productRepository.decreaseQuantity(productId, initialQuantity);

        // Reload from DB to verify the persisted state, not the entity
        // manager's local copy. Required because the JPQL @Modifying
        // update bypasses the persistence context.
        Optional<Product> reloaded = productRepository.findById(productId);

        // --- Assert ---------------------------------------------------------
        // Sanity: the row was eligible for the decrement (status ACTIVE,
        // not deleted, enough stock) so the UPDATE must have matched.
        assertThat(rowsUpdated)
                .as("decreaseQuantity on an ACTIVE product with quantity=%d, "
                                + "decrement=%d SHALL match exactly 1 row",
                        initialQuantity, initialQuantity)
                .isEqualTo(1);

        assertThat(reloaded)
                .as("Seeded ACTIVE product SHALL still be present after the UPDATE")
                .isPresent();

        assertThat(reloaded.get().getQuantity())
                .as("Quantity SHALL drop to 0 after subtracting the entire stock "
                        + "(initial=%d, decrement=%d)", initialQuantity, initialQuantity)
                .isZero();

        // P(10): zero-crossing SHALL flip status to SOLD. On unfixed code
        // the JPQL UPDATE only mutates p.quantity, so status stays ACTIVE
        // — that's the counterexample for the bug.
        assertThat(reloaded.get().getStatus())
                .as("Status of a product whose stock has just reached 0 SHALL be SOLD; "
                                + "actual status=%s (counterexample: JPQL UPDATE only sets "
                                + "p.quantity, never touches p.status when quantity hits 0)",
                        reloaded.get().getStatus())
                .isEqualTo(ProductStatus.SOLD);
    }
}
