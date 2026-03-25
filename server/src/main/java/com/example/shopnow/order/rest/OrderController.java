package com.example.shopnow.order.rest;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.order.OrderService;
import com.example.shopnow.order.rest.dto.CreateOrderRequest;
import com.example.shopnow.order.rest.dto.OrderDTO;
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
    public ResponseEntity<PageResponse<OrderDTO>> getOrers(
            @RequestParam(required = false, defaultValue = "1") int page,
            @AuthenticationPrincipal User viewer
        ) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.getOrders(pageable, viewer));
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
