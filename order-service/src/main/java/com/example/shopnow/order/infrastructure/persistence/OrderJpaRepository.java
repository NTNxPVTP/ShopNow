package com.example.shopnow.order.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.domain.models.Order;

/**
 * Spring Data repository for {@link Order}, confined to the
 * {@code infrastructure/persistence} package. It is an implementation detail of
 * {@link OrderRepositoryImpl} (the adapter that fulfils the pure domain
 * {@code OrderRepository} port) and carries the custom {@code @EntityGraph}
 * fetch hints that were previously declared on the package-level Spring Data
 * interface.
 */
@Repository
public interface OrderJpaRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    @Override
    @EntityGraph(attributePaths = "subOrders")
    Page<Order> findAll(Specification<Order> specification, Pageable pageable);

    @EntityGraph(attributePaths = { "subOrders", "subOrders.orderDetails" })
    Optional<Order> findWithDetailById(UUID id);

    /**
     * Paged search driven by a framework-neutral {@link OrderQuery}. The
     * {@code Specification} translation is performed here, inside
     * {@code infrastructure/persistence}, via {@link OrderSpecification#from},
     * so the application layer (e.g. {@code ListOrdersUseCase}) only constructs
     * an {@link OrderQuery} and never references {@code Specification}. The
     * paged fetch graph and ordering are preserved by delegating to the
     * {@code @EntityGraph}-annotated {@link #findAll(Specification, Pageable)}.
     *
     * @param query    the framework-neutral filter criteria ({@code null} = match-all)
     * @param pageable the paging / sorting request
     * @return the matching page of orders
     */
    default Page<Order> search(OrderQuery query, Pageable pageable) {
        return findAll(OrderSpecification.from(query), pageable);
    }
}
