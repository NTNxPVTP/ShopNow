package com.example.shopnow.product.domain.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    private String name;
    private String pictureUrl;
    private Integer quantity;
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ProductStatus status;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"), 
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreatedDate
    private LocalDateTime createdAt;
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Invariant: a product price is never {@code null} and never negative.
     * On a valid price the {@code price} field is updated and all other fields
     * are left unchanged.
     *
     * @param newPrice the new price; must be non-null and {@code >= 0}
     * @throws DomainException with {@link ErrorCode#PRODUCT_INVALID_PRICE} when
     *         {@code newPrice} is {@code null} or negative; in that case
     *         {@code price} (and every other field) is left unchanged.
     */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.signum() < 0) {
            throw new DomainException(ErrorCode.PRODUCT_INVALID_PRICE);
        }
        this.price = newPrice;
    }

    /**
     * Invariant: stock is never negative after a decrease. A positive quantity
     * is required; when there is not enough stock the quantity is left
     * unchanged and {@code false} is returned so the caller can react.
     *
     * @param qty the amount to remove from stock; must be {@code > 0}
     * @return {@code true} when stock was decreased by {@code qty};
     *         {@code false} when current {@code quantity} is {@code null} or
     *         less than {@code qty} (in which case {@code quantity} is unchanged)
     * @throws DomainException with {@link ErrorCode#PRODUCT_INVALID_QUANTITY}
     *         when {@code qty <= 0}; in that case {@code quantity} is unchanged.
     */
    public boolean decreaseStock(int qty) {
        if (qty <= 0) {
            throw new DomainException(ErrorCode.PRODUCT_INVALID_QUANTITY);
        }
        if (this.quantity == null || this.quantity < qty) {
            return false;
        }
        this.quantity = this.quantity - qty;
        return true;
    }

    /**
     * Soft-deletes the product by setting {@code isDeleted = true}, matching the
     * existing soft-delete behavior (the persistence layer flips the same
     * {@code is_deleted} flag and leaves {@code status} untouched).
     */
    public void markDeleted() {
        this.isDeleted = true;
    }

}
