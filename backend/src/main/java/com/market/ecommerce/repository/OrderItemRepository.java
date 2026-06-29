package com.market.ecommerce.repository;

import com.market.ecommerce.entity.OrderItem;
import com.market.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // استخدام JOIN FETCH لجلب المنتجات، واستخدام JOIN صريح لـ order لتجنب مشكلة الكلمة المحجوزة
    @Query("SELECT oi FROM OrderItem oi JOIN FETCH oi.product JOIN oi.order o WHERE o.status = :status")
    List<OrderItem> findByOrderStatus(@Param("status") OrderStatus status);

    // استعلام مخصص لجلب أزواج (رقم الطلب، رقم المنتج) مع تجنب الكلمة المحجوزة "order"
    @Query("SELECT o.id, p.id FROM OrderItem oi JOIN oi.order o JOIN oi.product p WHERE o.status = :status")
    List<Object[]> findOrderProductPairs(@Param("status") OrderStatus status);

    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) AS qty, SUM(oi.quantity * oi.price) AS revenue FROM OrderItem oi JOIN oi.order o WHERE o.status = :status GROUP BY oi.product.id, oi.product.name ORDER BY qty DESC")
    List<Object[]> findTopProductsByStatus(@Param("status") OrderStatus status, Pageable pageable);
}