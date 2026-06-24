package com.example.shopnow.product.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.shopnow.product.domain.models.Product;

/**
 * Driven port (outbound) for product persistence. It is a pure domain-level
 * abstraction: it intentionally carries no Spring Data
 * ({@code org.springframework.data.*}) nor JPA ({@code jakarta.persistence.*})
 * types so the {@code domain} and {@code application} layers depend only on this
 * interface, never on the persistence technology.
 *
 * <p>The concrete adapter lives in {@code infrastructure/persistence}
 * ({@code ProductRepositoryImpl}) and delegates to a Spring Data
 * {@code JpaRepository} ({@code ProductJpaRepository}) plus a product
 * {@code Specification} built from a {@link ProductQuery}. The atomic
 * stock-decrease ({@code UPDATE ... WHERE quantity >= :qty}) is preserved by
 * delegating {@link #decreaseQuantity(UUID, int)} to the JPQL
 * {@code @Modifying} query — it is never re-implemented in the port.
 */
public interface ProductRepository {

    /**
     * Persists the given product (insert or update) and returns the persisted
     * instance.
     *
     * @param product the product to save
     * @return the persisted product
     */
    Product save(Product product);

    /**
     * Finds a non-deleted product by id that is currently {@code ACTIVE}.
     *
     * @param id the product id
     * @return the active product if present, otherwise empty
     */
    Optional<Product> findActiveById(UUID id);

    /**
     * Finds a product by id that has not been soft-deleted, regardless of its
     * status.
     *
     * @param id the product id
     * @return the non-deleted product if present, otherwise empty
     */
    Optional<Product> findByIdAndNotDeleted(UUID id);

    /**
     * Finds a product by id, eagerly loading its owning shop.
     *
     * @param id the product id
     * @return the product with shop if present, otherwise empty
     */
    Optional<Product> findWithShopById(UUID id);

    /**
     * Soft-deletes the product owned by the given shop owner (sets
     * {@code isDeleted = true}) and returns the number of rows affected.
     *
     * @param id          the product id
     * @param shopOwnerId the owner of the shop the product belongs to
     * @return the number of rows updated (0 when not found / not owned)
     */
    int softDelete(UUID id, UUID shopOwnerId);

    /**
     * Atomically decreases the stock of the given product by {@code quantity}
     * using a single conditional {@code UPDATE ... WHERE quantity >= :quantity}
     * statement, so stock can never become negative and concurrent decrements
     * stay race-free. Returns the number of rows affected (0 when there was not
     * enough stock).
     *
     * @param id       the product id
     * @param quantity the amount to remove from stock
     * @return the number of rows updated (1 on success, 0 when not enough stock)
     */
    int decreaseQuantity(UUID id, int quantity);

    /**
     * Lists the {@code ACTIVE}, non-deleted products whose id is in the given
     * list, eagerly loading each product's owning shop.
     *
     * @param ids the product ids to look up
     * @return the matching products (possibly empty, never {@code null})
     */
    List<Product> findActiveWithShopByIds(List<UUID> ids);

    /**
     * Lists the products matching the given query criteria. Pagination is
     * handled by the calling use case / adapter; the port returns a plain
     * {@link List} to stay free of Spring Data paging types.
     *
     * @param query the filter criteria
     * @return the matching products (possibly empty, never {@code null})
     */
    List<Product> search(ProductQuery query);

    List<Product> findByShopId(UUID shopId);
}
