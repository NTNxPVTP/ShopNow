package com.example.shopnow.user;

import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.shopnow.user.models.Cart;
import java.util.List;


@Repository
public interface CartRepository extends JpaRepository<Cart,UUID> {
    @EntityGraph(attributePaths = "user")
    Cart findByName(String name);
}
