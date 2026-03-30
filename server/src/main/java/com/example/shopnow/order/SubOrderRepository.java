package com.example.shopnow.order;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import com.example.shopnow.order.models.SubOrder;

@Repository
interface SubOrderRepository extends JpaRepository<SubOrder, UUID>, JpaSpecificationExecutor<SubOrder> {
    @EntityGraph(attributePaths = {"orderDetails"})
    Optional<SubOrder> findWithDetailById(UUID id);

    @EntityGraph(attributePaths = {"orderDetails"})
    Optional<SubOrder> findWithDetailByIdAndShopOwnerId(UUID id, UUID shopOwnerId);

}
