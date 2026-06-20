package com.example.shopnow.order.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.example.shopnow.order.domain.models.Order;

/**
 * Driven port (outbound) for order persistence. It is a pure domain-level
 * abstraction: it intentionally carries no Spring Data
 * ({@code org.springframework.data.*}) nor JPA ({@code jakarta.persistence.*})
 * types so the {@code domain} and {@code application} layers depend only on this
 * interface, never on the persistence technology.
 *
 * <p>The concrete adapter lives in {@code infrastructure/persistence} and
 * delegates to a Spring Data {@code JpaRepository} plus an order
 * {@code Specification} built from an {@link OrderQuery}.
 */
public interface OrderRepository {

    /**
     * Persists the given order (insert or update) and returns the persisted
     * instance.
     *
     * @param order the order to save
     * @return the persisted order
     */
    Order save(Order order);

    /**
     * Finds an order by id, eagerly loading its sub-orders and order details.
     *
     * @param id the order id
     * @return the order with detail if present, otherwise empty
     */
    Optional<Order> findWithDetailById(UUID id);

    /**
     * Finds a single order by id without forcing detail loading.
     *
     * @param id the order id
     * @return the order if present, otherwise empty
     */
    Optional<Order> findById(UUID id);

    /**
     * Lists the orders matching the given query criteria. Pagination is handled
     * by the calling use case / adapter; the port returns a plain
     * {@link List} to stay free of Spring Data paging types.
     *
     * @param query the filter criteria
     * @return the matching orders (possibly empty, never {@code null})
     */
    List<Order> search(OrderQuery query);
}
