package com.example.shopnow.payment.domain.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.shopnow.payment.domain.models.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
}
