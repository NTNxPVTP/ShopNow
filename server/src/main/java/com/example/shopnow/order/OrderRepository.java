package com.example.shopnow.order;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.order.models.Order;
import com.example.shopnow.order.rest.dto.OrderItemRequest;

@Repository
interface OrderRepository extends JpaRepository<Order, UUID>{
    Page<Order> findWithPageReponseBy(Pageable pageable);
    List<OrderItemRequest> saveAlList(List<OrderItemRequest> list);
}
