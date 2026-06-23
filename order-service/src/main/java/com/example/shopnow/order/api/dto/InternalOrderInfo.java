package com.example.shopnow.order.api.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalOrderInfo {
    private UUID id;
    private UUID customerId;
    private BigDecimal totalPrice;
    private String status;
}
