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
import com.example.shopnow.order.domain.models.SubOrder;

/**
 * Spring Data repository for {@link SubOrder}, confined to the
 * {@code infrastructure/persistence} package. It is an implementation detail of
 * {@link SubOrderRepositoryImpl} (the adapter that fulfils the pure domain
 * {@code SubOrderRepository} port) and carries the custom {@code @EntityGraph}
 * fetch hints that were previously declared on the package-level Spring Data
 * interface.
 */
@Repository
public interface SubOrderJpaRepository extends JpaRepository<SubOrder, UUID>, JpaSpecificationExecutor<SubOrder> {

    @EntityGraph(attributePaths = { "orderDetails" })
    Optional<SubOrder> findWithDetailById(UUID id);

    @EntityGraph(attributePaths = { "orderDetails" })
    Optional<SubOrder> findWithDetailByIdAndShopOwnerId(UUID id, UUID shopOwnerId);

    /**
     * Paged search for the sub-orders owned by a given shop owner, driven by a
     * framework-neutral {@link OrderQuery}. The {@code Specification}
     * translation is performed here, inside {@code infrastructure/persistence},
     * by composing the {@code status} and {@code shopId} predicates from the
     * query with the {@code shopOwnerId} ownership constraint, so the
     * application layer (e.g. {@code ListSubOrdersUseCase}) only constructs an
     * {@link OrderQuery} plus the owner id and never references
     * {@code Specification}.
     *
     * <p>The {@code shopOwnerId} is kept out of {@link OrderQuery} (a query
     * object shared with order reads) and is supplied separately here.
     *
     * @param query       the framework-neutral filter criteria ({@code null} = match-all on status/shop)
     * @param shopOwnerId the owning shop owner the sub-orders must belong to
     * @param pageable    the paging / sorting request
     * @return the matching page of sub-orders
     */
    default Page<SubOrder> searchForOwner(OrderQuery query, UUID shopOwnerId, Pageable pageable) {
        Specification<SubOrder> spec = Specification.allOf(
                SubOrderSpecification.hasStatus(query == null ? null : query.status()),
                SubOrderSpecification.hasShopId(query == null ? null : query.shopId()),
                SubOrderSpecification.hasShopOwnerId(shopOwnerId));
        return findAll(spec, pageable);
    }
}
