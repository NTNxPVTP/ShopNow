package com.example.shopnow.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.shopnow.product.models.Product;
import com.example.shopnow.product.models.ProductStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {
    @Modifying
    @Query("""
            update Product p
            set p.isDeleted = true
            where p.id = :id and p.shop.ownerId = :shopOwnerId and p.isDeleted = false
            """)
    int softDeleteProductByIdAndShopOwnerId(@Param("id") UUID id, @Param("shopOwnerId") UUID shopOwnerId);

    Optional<Product> findByIdAndStatusAndIsDeletedFalse(UUID id, ProductStatus status);

    Optional<Product> findByIdAndIsDeletedFalse(UUID id);

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
}
