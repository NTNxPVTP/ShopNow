package com.example.shopnow.order.application.usecases;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.infrastructure.persistence.OrderJpaRepository;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that lists a customer's orders, paginated.
 *
 * <p>It expresses its filter purely as an {@link OrderQuery} (framework-neutral)
 * and never builds a Spring Data {@code Specification} itself — the
 * {@code Specification} translation stays confined to
 * {@code infrastructure/persistence} via
 * {@link OrderJpaRepository#search(OrderQuery, Pageable)}. The exact paged
 * behavior (fetch graph, ordering, page metadata) is preserved by delegating to
 * that infrastructure paging search and mapping with
 * {@link OrderMapper#toSummaryPageResponse}.
 *
 * <p>Exposes a single public execution entry point, {@link #execute}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListOrdersUseCase {

    private final OrderJpaRepository orderJpaRepository; // infrastructure paging search
    private final OrderMapper orderMapper;

    public PageResponse<OrderSummaryDTO> execute(Pageable pageable, AuthenticatedUser customer,
            OrderStatus status, UUID shopId) {
        if (customer == null || customer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // Application layer only constructs a framework-neutral query object;
        // the Specification is built inside the persistence layer.
        OrderQuery query = new OrderQuery(customer.getId(), status, shopId);

        Page<Order> orders = orderJpaRepository.search(query, pageable);
        return orderMapper.toSummaryPageResponse(orders);
    }
}
