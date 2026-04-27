package com.example.shopnow.product.models;

import java.util.Set;
import com.example.shopnow.shared.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "categories")
public class Category extends BaseEntity {
    private String name;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "categories")
    private Set<Product> products;
}
