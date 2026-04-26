package com.example.shopnow.user.models;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.shopnow.shared.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="carts")
public class Cart extends BaseEntity {
    private BigDecimal totalPrice;
    private Integer totalProduct;

    @OneToOne
    @JoinColumn(name = "customer_id")
    private User user;
}
