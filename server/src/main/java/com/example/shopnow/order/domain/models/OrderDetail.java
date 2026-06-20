package com.example.shopnow.order.domain.models;

import java.math.BigDecimal;
import java.util.UUID;
import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    /**
     * Factory that builds an {@link OrderDetail} from a resolved {@link OrderLine},
     * back-referencing the owning {@code subOrder}. The {@code productName},
     * unit {@code price} and {@code quantity} are copied from the line so the
     * persisted {@code product_name} column keeps the value the legacy create
     * path stored.
     *
     * @param subOrder the owning sub-order
     * @param line     the resolved order line
     * @return a new detail line for the given sub-order
     */
    public static OrderDetail of(SubOrder subOrder, OrderLine line) {
        return OrderDetail.builder()
                .subOrder(subOrder)
                .productId(line.productId())
                .productName(line.productName())
                .price(line.price())
                .quantity(line.quantity())
                .build();
    }

    /**
     * @return the monetary total for this detail line ({@code price × quantity}).
     */
    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
