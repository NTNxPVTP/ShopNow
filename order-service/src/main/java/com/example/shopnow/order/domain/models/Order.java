package com.example.shopnow.order.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.example.shopnow.exception.DomainException;
import com.example.shopnow.exception.ErrorCode;
import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Table(name = "orders")
@EntityListeners(AuditingEntityListener.class)
public class Order extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private OrderStatus status;
    private UUID customerId;
    private BigDecimal totalPrice;
    private String addressShipping;
    private String phoneNumber;
    private String customerName;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "order", cascade = CascadeType.ALL)
    private Set<SubOrder> subOrders;

    @CreatedDate
    private LocalDateTime createdAt;

    /**
     * Factory that builds a complete {@link Order} from resolved order lines.
     * Lines are grouped into one {@link SubOrder} per distinct {@code shopId};
     * every line is preserved (no loss, no duplication). The order starts in
     * {@link OrderStatus#IN_PROCESS} and its {@code totalPrice} is recalculated
     * from the resulting sub-orders.
     *
     * @throws DomainException with {@link ErrorCode#ORDER_INVALID_ITEMS} when
     *         {@code lines} is {@code null} or empty.
     */
    public static Order create(UUID customerId, String customerName, String phoneNumber,
            String addressShipping, List<OrderLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new DomainException(ErrorCode.ORDER_INVALID_ITEMS);
        }

        Order order = new Order();
        order.customerId = customerId;
        order.customerName = customerName;
        order.phoneNumber = phoneNumber;
        order.addressShipping = addressShipping;
        order.status = OrderStatus.PENDING_PAYMENT;
        order.subOrders = SubOrder.groupByShop(order, lines);
        order.recalculateTotal();
        return order;
    }

    /**
     * Invariant: {@code totalPrice} always equals the sum of the
     * {@code totalPrice} of every sub-order; {@code 0} when there are none.
     */
    public void recalculateTotal() {
        if (subOrders == null || subOrders.isEmpty()) {
            this.totalPrice = BigDecimal.ZERO;
            return;
        }
        this.totalPrice = subOrders.stream()
                .map(SubOrder::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Invariant: only allow status moves permitted by
     * {@link OrderStatus#canTransitionTo(OrderStatus)}. On an invalid move the
     * current {@code status} is left unchanged.
     *
     * @throws DomainException with {@link ErrorCode#ORDER_INVALID_TRANSITION}
     *         when the transition is not allowed.
     */
    public void transitionTo(OrderStatus target) {
        if (status == null || !status.canTransitionTo(target)) {
            throw new DomainException(ErrorCode.ORDER_INVALID_TRANSITION);
        }
        this.status = target;
    }

    public boolean isOwnedBy(UUID viewerId) {
        return customerId != null && customerId.equals(viewerId);
    }

}
