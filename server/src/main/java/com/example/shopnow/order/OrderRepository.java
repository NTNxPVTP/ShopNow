package com.example.shopnow.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.order.models.Order;

@Repository
interface OrderRepository extends JpaRepository<Order, UUID>{
    // @EntityGraph(attributePaths = { "subOrders", "subOrders.orderDetails"})
    Page<Order> findWithPageReponseAndDetailByCustomerId(Pageable pageable, UUID customerId);

    @EntityGraph(attributePaths = { "subOrders", "subOrders.orderDetails"})
    Optional<Order> findWithDetailById(UUID id);

    
}
