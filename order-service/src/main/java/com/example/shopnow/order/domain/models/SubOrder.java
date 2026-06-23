package com.example.shopnow.order.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sub_orders")
public class SubOrder extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "subOrder", cascade = CascadeType.ALL)
    private Set<OrderDetail> orderDetails;

    private UUID shopId;
    private BigDecimal totalPrice;
    private UUID shopOwnerId;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Groups the given {@code lines} into one {@link SubOrder} per distinct
     * {@code shopId}, preserving the encounter order of shops for deterministic
     * results. Every line is converted to an {@link OrderDetail} via
     * {@link OrderDetail#of(SubOrder, OrderLine)} and attached through
     * {@link #addDetail(OrderDetail)}; no line is lost or duplicated. Each
     * resulting sub-order starts in {@link OrderStatus#IN_PROCESS} and has its
     * {@code totalPrice} recalculated from its details.
     *
     * @param order the owning order to back-reference from each sub-order
     * @param lines the resolved order lines to group
     * @return the set of sub-orders, one per shop
     */
    public static Set<SubOrder> groupByShop(Order order, List<OrderLine> lines) {
        // Preserve encounter order of shops for deterministic grouping.
        Map<UUID, SubOrder> subOrdersByShop = new LinkedHashMap<>();

        for (OrderLine line : lines) {
            SubOrder subOrder = subOrdersByShop.computeIfAbsent(line.shopId(), shopId -> SubOrder.builder()
                    .order(order)
                    .shopId(shopId)
                    .shopOwnerId(line.shopOwnerId())
                    .status(OrderStatus.PENDING_PAYMENT)
                    .orderDetails(new HashSet<>())
                    .totalPrice(BigDecimal.ZERO)
                    .build());

            subOrder.addDetail(OrderDetail.of(subOrder, line));
        }

        return new LinkedHashSet<>(subOrdersByShop.values());
    }

    /**
     * Attaches a detail line to this sub-order, sets the back-reference to
     * {@code this}, and recalculates {@code totalPrice} so the invariant
     * ({@code totalPrice == Σ detail.lineTotal()}) is preserved.
     */
    public void addDetail(OrderDetail detail) {
        if (orderDetails == null) {
            orderDetails = new HashSet<>();
        }
        detail.setSubOrder(this);
        orderDetails.add(detail);
        recalculateTotal();
    }

    /**
     * Invariant: {@code totalPrice} always equals the sum of every detail's
     * {@code lineTotal()} ({@code price × quantity}); it is a non-negative value
     * and {@code 0} when there are no details.
     */
    public void recalculateTotal() {
        if (orderDetails == null || orderDetails.isEmpty()) {
            this.totalPrice = BigDecimal.ZERO;
            return;
        }
        this.totalPrice = orderDetails.stream()
                .map(OrderDetail::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
