package com.example.shopnow.order.application.usecases;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.repository.OrderQuery;
import com.example.shopnow.order.infrastructure.persistence.SubOrderJpaRepository;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that lists a seller's sub-orders, paginated.
 *
 * <p>It enforces the seller access guards, then expresses its filter purely as
 * an {@link OrderQuery} (framework-neutral) and never builds a Spring Data
 * {@code Specification} itself — the {@code Specification} translation,
 * including the {@code shopOwnerId} ownership constraint, stays confined to
 * {@code infrastructure/persistence} via
 * {@link SubOrderJpaRepository#searchForOwner(OrderQuery, UUID, Pageable)}. The
 * viewer's id is passed as the owner so a seller only sees their own
 * sub-orders, preserving the previous paged behavior; results are mapped with
 * {@link SubOrderMapper#toSummaryPageResponse}.
 *
 * <p>The {@code shopOwnerId} is intentionally not part of {@link OrderQuery}
 * (a shared order query object) and is supplied separately to the persistence
 * search.
 *
 * <p>Exposes a single public execution entry point, {@link #execute}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListSubOrdersUseCase {

    private final SubOrderJpaRepository subOrderJpaRepository; // infrastructure paging search
    private final SubOrderMapper subOrderMapper;

    public PageResponse<SubOrderSummaryDTO> execute(Pageable pageable, AuthenticatedUser viewer,
            OrderStatus status, UUID shopId) {
        if (viewer == null || viewer.getRole() == null || viewer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!viewer.getRole().equals("SELLER")) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        // Application layer only constructs a framework-neutral query object;
        // the Specification (including the shopOwnerId constraint) is built
        // inside the persistence layer.
        OrderQuery query = new OrderQuery(null, status, shopId);

        Page<SubOrder> subOrders = subOrderJpaRepository.searchForOwner(query, viewer.getId(), pageable);
        return subOrderMapper.toSummaryPageResponse(subOrders);
    }
}
