package com.example.shopnow.product;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
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
 * Exploration test for BUG-9 (ProductRepository.decreaseQuantity bỏ qua
 * {@code Product.status}, vẫn trừ tồn kho khi sản phẩm không còn ACTIVE).
 *
 * <p><b>Validates: Requirements 2.9</b> (Property 9 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug exists.
 * After the fix in {@link ProductRepository#decreaseQuantity} (adding
 * {@code AND p.status = ACTIVE} to the JPQL WHERE clause), this test must
 * turn green.
 *
 * <p><b>Bug condition C(9):</b> a row in {@code products} with
 * {@code status != ACTIVE} (canonical example: {@code SOLD}),
 * {@code isDeleted = false} and {@code quantity >= req.quantity}. The
 * production JPQL only filters on {@code quantity} and {@code isDeleted},
 * so the UPDATE matches and decrements the stock of a non-ACTIVE product.
 *
 * <p><b>Property P(9):</b> for any product {@code X} satisfying
 * {@code isBugCondition_9(X)},
 * {@link ProductRepository#decreaseQuantity(UUID, int)} SHALL return
 * {@code 0} (no row updated) and the persisted {@code quantity} of
 * {@code X} SHALL stay unchanged when reloaded.
 *
 * <p><b>Test design:</b> a Spring Data JPA slice ({@code @DataJpaTest})
 * with the embedded H2 datasource (replacing the production Postgres
 * {@code DB_URL}). Test scope is intentionally narrow: only the entities
 * touched by the WHERE clause ({@link Product} + its mandatory
 * {@link Shop} relation) are involved, and persistence is driven through
 * {@link EntityManager} so the test does not depend on
 * {@link ProductRepository#save} itself. Hibernate generates the schema
 * from entity annotations; H2 maps {@code @JdbcTypeCode(NAMED_ENUM)} to
 * VARCHAR, so {@code ProductStatus.SOLD} round-trips as the literal
 * {@code "SOLD"} that the JPQL guard will compare against.
 *
 * <p>The {@link ProductStatus} enum currently declares only {@code ACTIVE}
 * and {@code SOLD} (BUG-9 in {@code bugfix.md} mentions
 * {@code SOLD or INACTIVE} as canonical non-ACTIVE statuses, but the
 * codebase has not introduced {@code INACTIVE} yet); this test uses
 * {@link EnumSource} with {@code names = "ACTIVE", mode = EXCLUDE} so it
 * automatically covers any future non-ACTIVE status the enum gains
 * without code changes.
 *
 * <p><b>Expected counterexample on unfixed code:</b> seed a product with
 * {@code status = SOLD}, {@code isDeleted = false}, {@code quantity = 10};
 * call {@code productRepository.decreaseQuantity(id, 1)} → returns
 * {@code 1} and reloaded {@code quantity == 9} (assertion expects
 * {@code 0} / {@code 10}).
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
class ProductRepositoryBug9ExplorationTest {

    /** Initial stock seeded for the non-ACTIVE product under test. */
    private static final int INITIAL_QUANTITY = 10;

    /** Quantity asked to be decremented by the buggy call. */
    private static final int DECREMENT = 1;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest(name = "[{index}] status={0} -> decreaseQuantity returns 0 and quantity stays {1}")
    @EnumSource(value = ProductStatus.class, names = {"ACTIVE"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("decreaseQuantity SHALL refuse non-ACTIVE products (returns 0, stock unchanged)")
    void decreaseQuantity_nonActiveProduct_shallReturnZeroAndKeepStock(ProductStatus nonActiveStatus) {
        // --- Arrange --------------------------------------------------------
        Shop shop = new Shop();
        shop.setName("Bug9 Shop " + nonActiveStatus);
        shop.setOwnerId(UUID.randomUUID());
        shop.setIsActive(true);
        entityManager.persist(shop);

        Product product = Product.builder()
                .shop(shop)
                .name("Bug9 Product " + nonActiveStatus)
                .price(BigDecimal.valueOf(10))
                .quantity(INITIAL_QUANTITY)
                .status(nonActiveStatus)
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
        int rowsUpdated = productRepository.decreaseQuantity(productId, DECREMENT);

        // Reload from DB to verify the persisted state, not the entity
        // manager's local copy. Required because the JPQL @Modifying
        // update bypasses the persistence context.
        Optional<Product> reloaded = productRepository.findById(productId);

        // --- Assert ---------------------------------------------------------
        // P(9): non-ACTIVE products SHALL be rejected by decreaseQuantity.
        // On unfixed code rowsUpdated == 1 because the JPQL WHERE only
        // checks (quantity >= :quantity AND isDeleted = false) — the
        // counterexample for the bug.
        assertThat(rowsUpdated)
                .as("decreaseQuantity on a product with status=%s, isDeleted=false, "
                                + "quantity=%d SHALL return 0 (status guard refuses the update); "
                                + "actual rowsUpdated=%d (counterexample: JPQL WHERE clause "
                                + "is missing AND p.status = ACTIVE)",
                        nonActiveStatus, INITIAL_QUANTITY, rowsUpdated)
                .isZero();

        assertThat(reloaded)
                .as("Seeded product with status=%s SHALL still be present after the rejected update",
                        nonActiveStatus)
                .isPresent();
        assertThat(reloaded.get().getQuantity())
                .as("Stock of a status=%s product SHALL stay at %d after a rejected "
                                + "decreaseQuantity call; non-zero decrement here proves "
                                + "non-ACTIVE rows are reachable by the buggy UPDATE",
                        nonActiveStatus, INITIAL_QUANTITY)
                .isEqualTo(INITIAL_QUANTITY);
        assertThat(reloaded.get().getStatus())
                .as("Status of the seeded product SHALL stay %s (decreaseQuantity must not "
                                + "mutate status when the row was supposed to be untouched)",
                        nonActiveStatus)
                .isEqualTo(nonActiveStatus);
    }
}
