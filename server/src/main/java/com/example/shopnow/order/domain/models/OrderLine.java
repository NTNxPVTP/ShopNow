package com.example.shopnow.order.domain.models;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resolved order-line input used to build an {@link Order}. It carries the
 * product identity, the ordered {@code quantity}, the unit {@code price} already
 * resolved from the product catalog, and the {@code shopId} the product belongs
 * to (used to group lines into sub-orders).
 *
 * <p>It also carries the resolved {@code productName} and {@code shopOwnerId}
 * which are persisted on {@link OrderDetail} and {@link SubOrder} respectively,
 * preserving the columns the legacy create-order path used to populate.
 *
 * <p>This is a pure domain value type: it holds no JPA mapping and is never
 * persisted directly.
 */
public record OrderLine(UUID productId, String productName, Integer quantity, BigDecimal price,
        UUID shopId, UUID shopOwnerId) {

    /**
     * @return the monetary total for this line ({@code price × quantity}).
     */
    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
