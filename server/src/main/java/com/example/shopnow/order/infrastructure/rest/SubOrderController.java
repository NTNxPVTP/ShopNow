package com.example.shopnow.order.infrastructure.rest;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.order.application.usecases.GetSubOrderDetailUseCase;
import com.example.shopnow.order.application.usecases.ListSubOrdersUseCase;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subOrders")
public class SubOrderController {
    private final GetSubOrderDetailUseCase getSubOrderDetailUseCase;
    private final ListSubOrdersUseCase listSubOrdersUseCase;

    @GetMapping("/{id}")
    public ResponseEntity<SubOrderDTO> getSubOrderDetail(
        @PathVariable UUID id,
        @AuthenticationPrincipal AuthenticatedUser viewer
    ){
        return ResponseEntity.ok(getSubOrderDetailUseCase.execute(id, viewer));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SubOrderSummaryDTO>> getSubOrders(
        @AuthenticationPrincipal AuthenticatedUser viewer,
        @RequestParam(required = false, defaultValue = "1") int page,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) UUID shopId
    ){
        Pageable pageable = PageRequest.of(page-1, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(listSubOrdersUseCase.execute(pageable, viewer, status, shopId));
    }
    
}
