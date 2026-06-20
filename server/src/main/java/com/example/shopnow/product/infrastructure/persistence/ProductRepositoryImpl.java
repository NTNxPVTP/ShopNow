package com.example.shopnow.product.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.shopnow.product.domain.models.Product;
import com.example.shopnow.product.domain.models.ProductStatus;
import com.example.shopnow.product.domain.repository.ProductQuery;
import com.example.shopnow.product.domain.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter that fulfils the pure domain {@link ProductRepository}
 * port by delegating to the Spring Data {@link ProductJpaRepository} and
 * translating the framework-neutral {@link ProductQuery} into a
 * {@code Specification} via {@link ProductSpecification}. It contains no domain
 * business logic — every method is a thin delegation.
 *
 * <p>The atomic stock-decrease is preserved by delegating
 * {@link #decreaseQuantity(UUID, int)} to
 * {@link ProductJpaRepository#decreaseQuantity(UUID, int)}, whose JPQL
 * {@code UPDATE ... WHERE quantity >= :quantity} keeps stock from going below 0
 * and concurrent decrements race-free (Requirement 10.1, 10.3).
 */
@Repository
@RequiredArgsConstructor
class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpa;

    @Override
    public Product save(Product product) {
        return jpa.save(product);
    }

    @Override
    public Optional<Product> findActiveById(UUID id) {
        return jpa.findByIdAndStatusAndIsDeletedFalse(id, ProductStatus.ACTIVE);
    }

    @Override
    public Optional<Product> findByIdAndNotDeleted(UUID id) {
        return jpa.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public Optional<Product> findWithShopById(UUID id) {
        return jpa.findWithShopById(id);
    }

    @Override
    public int softDelete(UUID id, UUID shopOwnerId) {
        return jpa.softDeleteProductByIdAndShopOwnerId(id, shopOwnerId);
    }

    @Override
    public int decreaseQuantity(UUID id, int quantity) {
        return jpa.decreaseQuantity(id, quantity);
    }

    @Override
    public List<Product> findActiveWithShopByIds(List<UUID> ids) {
        return jpa.findAllWithShopByStatusAndIsDeletedFalseAndIdIn(ProductStatus.ACTIVE, ids);
    }

    @Override
    public List<Product> search(ProductQuery query) {
        return jpa.findAll(ProductSpecification.from(query));
    }
}
