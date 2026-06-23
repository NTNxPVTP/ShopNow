package com.example.shopnow.order.infrastructure.rest;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import com.example.shopnow.user.api.AuthUser;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.order.application.usecases.CreateOrderUseCase;
import com.example.shopnow.order.application.usecases.GetOrderDetailUseCase;
import com.example.shopnow.order.application.usecases.ListOrdersUseCase;
import com.example.shopnow.order.application.usecases.UpdateOrderStatusUseCase;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.api.AuthenticatedUser;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderDetailUseCase getOrderDetailUseCase;
    private final ListOrdersUseCase listOrdersUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryDTO>> getOrers(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID shopId,
            @AuthUser AuthenticatedUser viewer
        ) {
        Pageable pageable = PageRequest.of(page-1, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(listOrdersUseCase.execute(pageable, viewer, status, shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(
            @PathVariable UUID id,
            @AuthUser AuthenticatedUser viewer
        ) {
        return ResponseEntity.ok(getOrderDetailUseCase.execute(id, viewer));
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request, @AuthUser AuthenticatedUser buyer) {
        return ResponseEntity.ok(createOrderUseCase.execute(request, buyer));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam OrderStatus status,
            @AuthUser AuthenticatedUser viewer) {
        return ResponseEntity.ok(updateOrderStatusUseCase.execute(id, status, viewer));
    }

}

