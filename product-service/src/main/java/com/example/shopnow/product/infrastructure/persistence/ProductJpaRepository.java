package com.example.shopnow.product.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.repository.ProductQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data repository for {@link Product}, confined to the
 * {@code infrastructure/persistence} package. It is an implementation detail of
 * {@link ProductRepositoryImpl} (the adapter that fulfils the pure domain
 * {@code ProductRepository} port) and carries every custom query that was
 * previously declared on the package-level {@code ProductRepository} Spring Data
 * interface — including the two {@code @Modifying @Query} statements (soft
 * delete and the atomic {@code decreaseQuantity}) copied verbatim so the
 * race-free {@code UPDATE ... WHERE quantity >= :quantity} behaviour is
 * preserved exactly.
 */
@Repository
public interface ProductJpaRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    @Modifying
    @Query("""
            update Product p
            set p.isDeleted = true
            where p.id = :id and p.shop.ownerId = :shopOwnerId and p.isDeleted = false
            """)
    int softDeleteProductByIdAndShopOwnerId(@Param("id") UUID id, @Param("shopOwnerId") UUID shopOwnerId);

    Optional<Product> findByIdAndStatusAndIsDeletedFalse(UUID id, ProductStatus status);

    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

    @EntityGraph(attributePaths = { "categories" })
    List<Product> findByShopIdAndIsDeletedFalse(UUID shopId);

    Page<Product> findWithPageReponseBy(Pageable pageable);

    @EntityGraph(attributePaths = { "shop" })
    List<Product> findAllWithShopByStatusAndIsDeletedFalseAndIdIn(ProductStatus status, List<UUID> ids);

    @Modifying
    @Query("Update Product p " +
            "set p.quantity = p.quantity - :quantity " +
            "where p.quantity >= :quantity and p.id = :id and p.isDeleted = false")
    int decreaseQuantity(@Param("id") UUID id, @Param("quantity") int quantity);

    @EntityGraph(attributePaths = { "shop" })
    Optional<Product> findWithShopById(UUID id);

    /**
     * Paged search driven by a framework-neutral {@link ProductQuery}. The
     * {@code Specification} translation is performed here, inside
     * {@code infrastructure/persistence}, via
     * {@link ProductSpecification#from(ProductQuery)}, so the application layer
     * (e.g. {@code ListProductsUseCase}) only constructs a {@link ProductQuery}
     * and never references {@code Specification}. The exact spec composition is
     * preserved — {@code ACTIVE} status, {@code isNotDeleted}, and the
     * conditional {@code isInStock} when {@link ProductQuery#inStockOnly()} —
     * and the paging / sorting request is honoured by delegating to
     * {@link #findAll(Specification, Pageable)}.
     *
     * @param query    the framework-neutral filter criteria ({@code null} = match-all non-deleted)
     * @param pageable the paging / sorting request
     * @return the matching page of products
     */
    default Page<Product> search(ProductQuery query, Pageable pageable) {
        return findAll(ProductSpecification.from(query), pageable);
    }
}
