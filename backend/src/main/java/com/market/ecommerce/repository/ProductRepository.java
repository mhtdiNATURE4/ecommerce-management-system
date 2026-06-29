package com.market.ecommerce.repository;

import com.market.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // تمت إزالة OrderByNameAsc لتجنب أي تعارض مع خصائص Pageable
    Page<Product> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    java.util.List<Product> findAllWithCategory();

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategory(Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"category"})
    java.util.List<Product> findByStockLessThanEqualOrderByStockAsc(Integer threshold);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock - :qty WHERE p.id = :id AND p.stock >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.stock = p.stock + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    @Query("SELECT p.stock FROM Product p WHERE p.id = :id")
    Integer findStockById(@Param("id") Long id);

    // Keep findById from JpaRepository for optimistic locking usage

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    java.util.Optional<com.market.ecommerce.entity.Product> findByIdForUpdate(@Param("id") Long id);
}
