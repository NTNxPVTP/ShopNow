package com.example.shopnow.order.infrastructure.rest;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.order.api.dto.InternalOrderInfo;
import com.example.shopnow.order.domain.models.Order;
import com.example.shopnow.order.domain.models.OrderStatus;
import com.example.shopnow.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderRepository orderRepository;

    @GetMapping("/{id}")
    public ResponseEntity<InternalOrderInfo> getOrderInfo(@PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new DomainException(ErrorCode.ORDER_NOT_FOUND));

        InternalOrderInfo info = new InternalOrderInfo(
                order.getId(),
                order.getCustomerId(),
                order.getTotalPrice(),
                order.getStatus().name()
        );
        return ResponseEntity.ok(info);
    }


}
