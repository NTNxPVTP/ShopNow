package com.example.shopnow.product;

import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;
import com.example.shopnow.product.models.Shop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exploration test for BUG-11 (JPQL {@code @Modifying} updates in
 * {@link ProductRepository} bypass the JPA lifecycle listener
 * {@code AuditingEntityListener}, so the {@code updatedAt} column never
 * gets refreshed even though the row's data was mutated).
 *
 * <p><b>Validates: Requirements 2.11</b> (Property 11 in design.md).
 *
 * <p>Phase 1 bug-condition exploration test for the bugfix workflow
 * {@code shopnow-codebase-bugfixes}. It is EXPECTED TO FAIL on unfixed
 * code; the failure surfaces a counterexample that proves the bug exists.
 * After the fix in {@link ProductRepository} (each JPQL UPDATE
 * additionally setting {@code p.updatedAt = CURRENT_TIMESTAMP}), this
 * test must turn green.
 *
 * <p><b>Bug condition C(11):</b> a repository call
 * {@code repoCall ∈ { decreaseQuantity, softDeleteProductByIdAndShopOwnerId }}.
 * Both methods are declared with {@code @Modifying @Query("update Product
 * p set ...")}; Hibernate executes them as a bulk UPDATE statement that
 * intentionally bypasses the persistence context, which means the
 * {@code @PreUpdate} hook owned by Spring Data's
 * {@code AuditingEntityListener} (the source of truth for
 * {@code @LastModifiedDate}) is never invoked.
 *
 * <p><b>Property P(11):</b> for any JPQL update {@code X ∈
 * { decreaseQuantity, softDeleteProductByIdAndShopOwnerId }} that
 * matches an existing row, the persisted {@code updatedAt} after the
 * call SHALL be strictly later than the {@code updatedAt} captured
 * immediately before the call (using
 * {@link LocalDateTime#isAfter(java.time.chrono.ChronoLocalDateTime)}).
 *
 * <p><b>Test design:</b> a Spring Data JPA slice ({@code @DataJpaTest})
 * with the embedded H2 datasource (replacing the production Postgres
 * {@code DB_URL}). Same wiring as the BUG-9 / BUG-10 sibling tests
 * ({@link ProductRepositoryBug9ExplorationTest},
 * {@link ProductRepositoryBug10ExplorationTest}) — only the entities
 * touched by the WHERE clause ({@link Product} + its mandatory
 * {@link Shop} relation) are involved, persistence is driven through
 * {@link EntityManager}, and the {@link TestH2EnumFriendlyDialect} keeps
 * the Postgres {@code @JdbcTypeCode(NAMED_ENUM)} mapping schema-compatible
 * with H2.
 *
 * <p>Two extra tweaks specific to BUG-11:
 * <ul>
 *   <li>{@code @EnableJpaAuditing} is re-enabled inside the slice via the
 *       inner {@link AuditingTestConfig}, because {@code @DataJpaTest}
 *       does not pick up the production {@code ApplicationConfig}, and
 *       without an active auditing handler {@code @CreatedDate} /
 *       {@code @LastModifiedDate} would stay {@code null} on the seed
 *       product — the test could not capture {@code updatedAt_before}.</li>
 *   <li>A short {@link Thread#sleep(long) sleep} between the seed and the
 *       JPQL UPDATE guarantees the system clock has advanced past the
 *       persisted {@code updatedAt_before} timestamp; without it the fix
 *       could legitimately land on the same nanosecond and produce a
 *       false negative.</li>
 * </ul>
 *
 * <p><b>Expected counterexample on unfixed code:</b> seed an ACTIVE
 * product with {@code shop.ownerId} known, flush so auditing sets
 * {@code updatedAt = T0}; sleep a few millis; call the JPQL update →
 * row is mutated (rowsUpdated == 1) but reloaded {@code updatedAt} still
 * equals {@code T0} (assertion expects strictly later than {@code T0}).
 */
@DataJpaTest
// Force H2 even though the project also has Postgres on the runtime
// classpath; a DataJpaTest slice would otherwise try to connect to the
// configured production DB_URL and fail outside the integration env.
@AutoConfigureTestDatabase(replace = Replace.ANY)
@Import(ProductRepositoryBug11ExplorationTest.AuditingTestConfig.class)
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
class ProductRepositoryBug11ExplorationTest {

    /**
     * Re-enables Spring Data JPA auditing inside the {@code @DataJpaTest}
     * slice. The production {@link com.example.shopnow.config.ApplicationConfig}
     * carries the {@code @EnableJpaAuditing} annotation, but
     * {@code @DataJpaTest} only autoconfigures JPA repositories and does
     * not import the rest of the application's configuration. Without
     * this override the {@code AuditingEntityListener} attached to
     * {@link Product} would silently no-op and the test could not capture
     * a non-null {@code updatedAt_before}.
     */
    @TestConfiguration
    @EnableJpaAuditing
    static class AuditingTestConfig {
    }

    /**
     * Sleep duration between the seed (auditing stamps
     * {@code updatedAt_before}) and the JPQL UPDATE. Long enough that
     * {@code LocalDateTime.now()} on the next call is strictly greater
     * even on platforms where the system clock has milli-second
     * resolution; short enough to keep the test fast.
     */
    private static final long CLOCK_TICK_MILLIS = 20L;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("decreaseQuantity SHALL refresh updatedAt (JPQL @Modifying must not bypass auditing)")
    void decreaseQuantity_shallRefreshUpdatedAt() throws InterruptedException {
        // --- Arrange --------------------------------------------------------
        Shop shop = new Shop();
        shop.setName("Bug11 Shop decreaseQuantity");
        shop.setOwnerId(UUID.randomUUID());
        shop.setIsActive(true);
        entityManager.persist(shop);

        Product product = Product.builder()
                .shop(shop)
                .name("Bug11 Product decreaseQuantity")
                .price(BigDecimal.valueOf(10))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();
        entityManager.persist(product);
        // Force INSERT so the auditing listener stamps createdAt /
        // updatedAt and the row is visible to the bulk UPDATE below.
        entityManager.flush();
        UUID productId = product.getId();
        LocalDateTime updatedAtBefore = product.getUpdatedAt();

        // Sanity: auditing must be active inside the slice — otherwise the
        // bug condition is unobservable (we couldn't tell whether the
        // JPQL UPDATE refreshed the column or not).
        assertThat(updatedAtBefore)
                .as("Auditing SHALL stamp Product.updatedAt on the seed insert; "
                        + "@EnableJpaAuditing must be active in the test slice")
                .isNotNull();

        // Detach so the next read after the UPDATE pulls fresh DB state
        // rather than the in-memory snapshot.
        entityManager.clear();

        // Wait for the wall clock to advance past updatedAt_before so any
        // refresh of updatedAt produces a strictly later timestamp.
        Thread.sleep(CLOCK_TICK_MILLIS);

        // --- Act ------------------------------------------------------------
        int rowsUpdated = productRepository.decreaseQuantity(productId, 1);

        Product reloaded = productRepository.findById(productId).orElseThrow();
        LocalDateTime updatedAtAfter = reloaded.getUpdatedAt();

        // --- Assert ---------------------------------------------------------
        // Sanity: the UPDATE actually matched the seeded row, so a
        // refresh of updatedAt is genuinely expected.
        assertThat(rowsUpdated)
                .as("decreaseQuantity on a seeded ACTIVE product SHALL match exactly 1 row")
                .isEqualTo(1);

        // P(11): the JPQL @Modifying update SHALL refresh updatedAt. On
        // unfixed code the bulk UPDATE bypasses AuditingEntityListener,
        // so updatedAt stays equal to updatedAt_before — counterexample.
        assertThat(updatedAtAfter)
                .as("Product.updatedAt after decreaseQuantity SHALL be strictly later than "
                                + "before the call (before=%s); actual after=%s "
                                + "(counterexample: JPQL @Modifying bypasses AuditingEntityListener)",
                        updatedAtBefore, updatedAtAfter)
                .isNotNull()
                .isAfter(updatedAtBefore);
    }

    @Test
    @DisplayName("softDeleteProductByIdAndShopOwnerId SHALL refresh updatedAt (JPQL @Modifying must not bypass auditing)")
    void softDeleteProductByIdAndShopOwnerId_shallRefreshUpdatedAt() throws InterruptedException {
        // --- Arrange --------------------------------------------------------
        UUID shopOwnerId = UUID.randomUUID();
        Shop shop = new Shop();
        shop.setName("Bug11 Shop softDelete");
        shop.setOwnerId(shopOwnerId);
        shop.setIsActive(true);
        entityManager.persist(shop);

        Product product = Product.builder()
                .shop(shop)
                .name("Bug11 Product softDelete")
                .price(BigDecimal.valueOf(10))
                .quantity(10)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();
        entityManager.persist(product);
        entityManager.flush();
        UUID productId = product.getId();
        LocalDateTime updatedAtBefore = product.getUpdatedAt();

        assertThat(updatedAtBefore)
                .as("Auditing SHALL stamp Product.updatedAt on the seed insert; "
                        + "@EnableJpaAuditing must be active in the test slice")
                .isNotNull();

        entityManager.clear();

        Thread.sleep(CLOCK_TICK_MILLIS);

        // --- Act ------------------------------------------------------------
        int rowsUpdated = productRepository.softDeleteProductByIdAndShopOwnerId(productId, shopOwnerId);

        Product reloaded = productRepository.findById(productId).orElseThrow();
        LocalDateTime updatedAtAfter = reloaded.getUpdatedAt();

        // --- Assert ---------------------------------------------------------
        // Sanity: the UPDATE actually matched the seeded row, so a
        // refresh of updatedAt is genuinely expected. Also confirm the
        // intended side effect (isDeleted flipped to true) so we don't
        // accidentally pass the property by hitting a no-op UPDATE.
        assertThat(rowsUpdated)
                .as("softDeleteProductByIdAndShopOwnerId on a seeded product owned by "
                        + "shopOwnerId=%s SHALL match exactly 1 row", shopOwnerId)
                .isEqualTo(1);
        assertThat(reloaded.isDeleted())
                .as("softDeleteProductByIdAndShopOwnerId SHALL flip isDeleted to true")
                .isTrue();

        // P(11): the JPQL @Modifying update SHALL refresh updatedAt. On
        // unfixed code the bulk UPDATE bypasses AuditingEntityListener,
        // so updatedAt stays equal to updatedAt_before — counterexample.
        assertThat(updatedAtAfter)
                .as("Product.updatedAt after softDeleteProductByIdAndShopOwnerId SHALL be "
                                + "strictly later than before the call (before=%s); actual after=%s "
                                + "(counterexample: JPQL @Modifying bypasses AuditingEntityListener)",
                        updatedAtBefore, updatedAtAfter)
                .isNotNull()
                .isAfter(updatedAtBefore);
    }
}
