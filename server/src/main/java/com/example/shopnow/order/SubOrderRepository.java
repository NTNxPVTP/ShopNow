package com.example.shopnow.order;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.order.models.SubOrder;

@Repository
interface SubOrderRepository extends JpaRepository<SubOrder, UUID> {
    @EntityGraph(attributePaths = {"orderDetails"})
    Optional<SubOrder> findWithDetailById(UUID id);

    @EntityGraph(attributePaths = {"orderDetails"})
    Optional<SubOrder> findWithDetailByIdAndShopOwnerId(UUID id, UUID shopOwnerId);

    Page<SubOrder> findWithPageResponseByShopOwnerId(Pageable pageable, UUID shopOwnerId);
}
