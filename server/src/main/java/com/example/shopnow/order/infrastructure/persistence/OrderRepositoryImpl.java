package com.example.shopnow.order.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.domain.repository.OrderRepository;
import com.example.shopnow.order.domain.models.Order;

import lombok.RequiredArgsConstructor;

/**
 * Persistence adapter that fulfils the pure domain {@link OrderRepository} port
 * by delegating to the Spring Data {@link OrderJpaRepository} and translating
 * the framework-neutral {@link OrderQuery} into a {@code Specification} via
 * {@link OrderSpecification}. It contains no domain business logic — every
 * method is a thin delegation.
 */
@Repository
@RequiredArgsConstructor
class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository jpa;

    @Override
    public Order save(Order order) {
        return jpa.save(order);
    }

    @Override
    public Optional<Order> findWithDetailById(UUID id) {
        return jpa.findWithDetailById(id);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<Order> search(OrderQuery query) {
        return jpa.findAll(OrderSpecification.from(query));
    }
}
