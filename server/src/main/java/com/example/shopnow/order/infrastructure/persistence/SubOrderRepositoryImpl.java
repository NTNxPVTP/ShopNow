package com.example.shopnow.order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.domain.repository.SubOrderRepository;
import com.example.shopnow.order.domain.models.SubOrder;

import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter that fulfils the pure domain {@link SubOrderRepository}
 * port by delegating to the Spring Data {@link SubOrderJpaRepository} and
 * composing the sub-order {@code Specification} from the framework-neutral
 * {@link OrderQuery}. It contains no domain business logic — every method is a
 * thin delegation.
 *
 * <p>{@code OrderQuery} carries {@code status} and {@code shopId} (the owning
 * {@code shopOwnerId} is not part of the query object), so {@link #search}
 * composes only those two predicates.
 */
@Repository
@RequiredArgsConstructor
class SubOrderRepositoryImpl implements SubOrderRepository {

    private final SubOrderJpaRepository jpa;

    @Override
    public Optional<SubOrder> findWithDetailById(UUID id) {
        return jpa.findWithDetailById(id);
    }

    @Override
    public Optional<SubOrder> findWithDetailByIdAndShopOwnerId(UUID id, UUID shopOwnerId) {
        return jpa.findWithDetailByIdAndShopOwnerId(id, shopOwnerId);
    }

    @Override
    public List<SubOrder> search(OrderQuery query) {
        return jpa.findAll(toSpecification(query));
    }

    private Specification<SubOrder> toSpecification(OrderQuery query) {
        if (query == null) {
            return Specification.allOf();
        }
        return Specification.allOf(
                SubOrderSpecification.hasStatus(query.status()),
                SubOrderSpecification.hasShopId(query.shopId()));
    }
}
