package com.example.shopnow.product.models;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import com.example.shopnow.shared.BaseEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "shops")
public class Shop extends BaseEntity{
    private String name;
    private String address;
    private String avatarUrl;
    private Boolean isActive;

    @CreatedDate
    private LocalDateTime createdAt;
}
