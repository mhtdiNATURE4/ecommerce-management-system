package com.market.ecommerce.repository;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // دالتك الأصلية التي تدعم الـ Pagination (ممتازة للـ Frontend)
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // الدالة التي أضفناها لدعم OrderService
    List<Order> findByUserId(Long userId);

    // الدالة التي أضفناها لدعم DashboardService و CustomerSegmentationService
    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.user WHERE o.id = :id")
    Optional<Order> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.user.id = :userId")
    java.util.List<Order> findByUserIdWithItems(@Param("userId") Long userId);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product")
    java.util.List<Order> findAllWithItems();

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.status = :status")
    java.util.List<Order> findByStatusWithItems(@Param("status") OrderStatus status);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product WHERE o.id IN :ids")
    java.util.List<Order> findByIdInWithItems(@Param("ids") java.util.List<Long> ids);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.idempotencyKey = :key")
    java.util.Optional<Order> findByUserIdAndIdempotencyKey(@Param("userId") Long userId, @Param("key") String key);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    java.util.Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE orders SET status = 'PROCESSING' WHERE id = :id AND status NOT IN ('PROCESSING','COMPLETED','CANCELLED')", nativeQuery = true)
    int markOrderProcessingIfNotTerminal(@Param("id") Long id);
}
