package com.market.ecommerce.repository;

import com.market.ecommerce.entity.CartItem;
import com.market.ecommerce.entity.Product;
import com.market.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"product"})
    List<CartItem> findByUser(User user);

    Optional<CartItem> findByUserAndProduct(User user, Product product);

    // تحسين الأداء عبر استخدام Bulk Delete بدلاً من الحذف الفردي
    @Modifying
    @Transactional
    @Query("DELETE FROM CartItem c WHERE c.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}