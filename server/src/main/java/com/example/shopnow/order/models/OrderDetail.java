package com.example.shopnow.order.models;

import java.math.BigDecimal;
import java.util.UUID;
import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "order_detail")
public class OrderDetail extends BaseEntity{

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_order_id")
    private SubOrder subOrder;
    private String productName;
    private UUID productId;
    private BigDecimal price;
    private Integer quantity;
}
