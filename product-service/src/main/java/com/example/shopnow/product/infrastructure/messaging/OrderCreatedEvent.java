package com.example.shopnow.product.infrastructure.messaging;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.example.shopnow.product.api.dto.OrderLineRequest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private List<OrderLineRequest> items;
}
