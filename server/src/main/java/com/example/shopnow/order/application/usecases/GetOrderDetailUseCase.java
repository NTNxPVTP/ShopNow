package com.example.shopnow.order.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.repository.OrderRepository;
import com.example.shopnow.order.mapper.OrderMapper;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that reads a single order's detail for a viewer.
 *
 * <p>It is pure orchestration: it loads the order through the
 * {@link OrderRepository} driven port and delegates the ownership invariant to
 * the domain via {@link Order#isOwnedBy(UUID)}. When the viewer is not the
 * owner the use case throws {@code ORDER_ACCESS_DENIED} and returns no order
 * data; when the order does not exist it throws {@code ORDER_NOT_FOUND}.
 *
 * <p>Exposes a single public execution entry point, {@link #execute}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetOrderDetailUseCase {

    private final OrderRepository orderRepository; // driven port (intra-module)
    private final OrderMapper orderMapper;

    public OrderDTO execute(UUID id, AuthenticatedUser viewer) {
        Order order = orderRepository.findWithDetailById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        // Ownership invariant is delegated to the domain model. On a failed
        // check we deny access and return no order data.
        if (!order.isOwnedBy(viewer.getId())) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        return orderMapper.toDto(order);
    }
}
