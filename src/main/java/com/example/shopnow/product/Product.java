package com.example.shopnow.product;

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
    private Integer id;
    private Integer shop_id;
    private String name;
    private String picture_url;
    private Integer quantity;
    private Double price;
    private String status;
    private String created_at;
    private String updated_at;
}
