package com.example.shopnow.order.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.example.shopnow.order.domain.models.SubOrder;

/**
 * Driven port (outbound) for sub-order persistence. Like {@link OrderRepository}
 * it is a pure domain-level abstraction with no Spring Data
 * ({@code org.springframework.data.*}) nor JPA ({@code jakarta.persistence.*})
 * types.
 *
 * <p>The concrete adapter lives in {@code infrastructure/persistence} and
 * delegates to a Spring Data {@code JpaRepository} plus a sub-order
 * {@code Specification} built from an {@link OrderQuery}.
 */
public interface SubOrderRepository {

    /**
     * Finds a sub-order by id, eagerly loading its order details.
     *
     * @param id the sub-order id
     * @return the sub-order with detail if present, otherwise empty
     */
    Optional<SubOrder> findWithDetailById(UUID id);

    /**
     * Finds a sub-order by id scoped to the owning shop, eagerly loading its
     * order details. Used to enforce that a seller only reads their own
     * sub-orders.
     *
     * @param id          the sub-order id
     * @param shopOwnerId the id of the shop owner the sub-order must belong to
     * @return the sub-order with detail if present and owned, otherwise empty
     */
    Optional<SubOrder> findWithDetailByIdAndShopOwnerId(UUID id, UUID shopOwnerId);

    /**
     * Lists the sub-orders matching the given query criteria. Pagination is
     * handled by the calling use case / adapter; the port returns a plain
     * {@link List} to stay free of Spring Data paging types.
     *
     * @param query the filter criteria
     * @return the matching sub-orders (possibly empty, never {@code null})
     */
    List<SubOrder> search(OrderQuery query);
}
