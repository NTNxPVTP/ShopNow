package com.example.shopnow.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.example.shopnow.product.models.Product;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.query.Param;


@Repository
public interface ProductRepository extends JpaRepository<Product, UUID > {
    @Modifying
    @Query("Delete from Product p where p.id = :id")
    int deleteProductById(@Param("id") UUID id);

    Page<Product> findWithPageReponseBy(Pageable pageable);

    List<Product> findAllByIdIn(List<UUID> ids);
    
    @Modifying
    @Query("Update Product p " +
            "set p.quantity = p.quantity - :quantity " +        
            "where p.quantity >= :quantity and p.id = :id"
    )
    int decreaseQuantity(@Param("id") UUID id,@Param("quantity") int quantity);
}
    