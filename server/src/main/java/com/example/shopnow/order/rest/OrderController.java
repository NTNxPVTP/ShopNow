package com.example.shopnow.order.rest;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.order.OrderService;
import com.example.shopnow.order.models.OrderStatus;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
import com.example.shopnow.order.rest.dto.OrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.User;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    @GetMapping
    public ResponseEntity<PageResponse<OrderSummaryDTO>> getOrers(
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) UUID shopId,
            @AuthenticationPrincipal User viewer
        ) {
        Pageable pageable = PageRequest.of(page-1, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.getOrders(pageable, viewer, status, shopId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal User viewer
        ) {
        return ResponseEntity.ok(service.getOrderDetail(id,viewer));
    }

    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@Valid @RequestBody CreateOrderRequest request, @AuthenticationPrincipal User buyer) {
        return ResponseEntity.ok(service.createOrder(request, buyer));
    }

}
