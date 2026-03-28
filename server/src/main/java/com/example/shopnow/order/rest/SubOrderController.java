package com.example.shopnow.order.rest;

import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.shopnow.order.OrderService;
import com.example.shopnow.order.rest.dto.SubOrderDTO;
import com.example.shopnow.order.rest.dto.SubOrderSummaryDTO;
import com.example.shopnow.shared.PageResponse;
import com.example.shopnow.user.models.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subOrders")
public class SubOrderController {
    private final OrderService service;

    @GetMapping("/{id}")
    public ResponseEntity<SubOrderDTO> getSubOrderDetail(
        @PathVariable UUID id,
        @AuthenticationPrincipal User viewer
    ){
        System.out.println("Call service here: ");
        return ResponseEntity.ok(service.getSubOrderDetail(id, viewer));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SubOrderSummaryDTO>> getSubOrders(
        @AuthenticationPrincipal User viewer,
        @RequestParam(required = false, defaultValue = "1") int page
    ){
        Pageable pageable = PageRequest.of(page-1, 10, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.getSubOrders(pageable,viewer));
    }
    
}
