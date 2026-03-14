package com.example.shopnow.product;

import java.util.UUID;

import org.springframework.data.mapping.model.BasicPersistentEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue
    private UUID id;
    private UUID shop_id;
    private String name;
    private String picture_url;
    private Integer quantity;
    private Double price;
    private String status;
    private String created_at;
    private String updated_at;
}
