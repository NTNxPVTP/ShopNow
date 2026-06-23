package com.example.shopnow.order.application.usecases;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.domain.repository.SubOrderRepository;
import com.example.shopnow.order.mapper.SubOrderMapper;
import com.example.shopnow.order.domain.models.SubOrder;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

/**
 * Use case (driving port) that reads a single sub-order's detail for a seller
 * viewer.
 *
 * <p>It is pure orchestration: it enforces the seller access guards and then
 * loads the sub-order through the {@link SubOrderRepository} driven port scoped
 * to the viewer's shop. A viewer that is missing identity/role, or is not a
 * {@code SELLER}, is denied with {@code ORDER_ACCESS_DENIED}; a sub-order that
 * does not exist (or is not owned by the viewer) yields {@code ORDER_NOT_FOUND}.
 *
 * <p>Exposes a single public execution entry point, {@link #execute}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSubOrderDetailUseCase {

    private final SubOrderRepository subOrderRepository; // driven port (intra-module)
    private final SubOrderMapper subOrderMapper;

    public SubOrderDTO execute(UUID id, AuthenticatedUser viewer) {
        if (viewer == null || viewer.getRole() == null || viewer.getId() == null) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        if (!viewer.getRole().equals("SELLER")) {
            throw new DomainException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        SubOrder subOrder = subOrderRepository.findWithDetailByIdAndShopOwnerId(id, viewer.getId())
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        return subOrderMapper.toDto(subOrder);
    }
}
