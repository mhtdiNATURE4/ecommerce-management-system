package com.market.ecommerce.repository;

import com.market.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    boolean existsByOrder_Id(Long orderId);

    java.util.Optional<Payment> findByOrder_Id(Long orderId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE payments SET status = 'COMPLETED', transaction_id = :tx WHERE id = :id AND status = 'PENDING'", nativeQuery = true)
    int finalizePaymentIfPending(@Param("id") Long id, @Param("tx") String tx);
}
