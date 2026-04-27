package com.example.shopnow.product;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopnow.product.models.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Set<Category> findAllByIdIn(Set<UUID> ids);

    boolean existsByNameIgnoreCase(String name);
}
