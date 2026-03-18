package com.example.shopnow.order.rest;

import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.shopnow.order.OrderService;
import com.example.shopnow.order.rest.dto.OrderDetail;
import com.example.shopnow.shared.PageResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;

    @GetMapping
    public ResponseEntity<PageResponse<OrderDetail>> getOrers(
        @RequestParam(required = false, defaultValue = "1") int page
    ){
        Pageable pageable= PageRequest.of(page, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.getOrders(pageable)); 
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetail> getOrderDetail(
        @PathVariable UUID id
    ){
        return ResponseEntity.ok(service.getOrderDetail(id));
    }
}
