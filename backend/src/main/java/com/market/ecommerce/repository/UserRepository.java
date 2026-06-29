package com.market.ecommerce.repository;

import com.market.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // إضافة للتحقق من وجود البريد الإلكتروني قبل إنشاء الحساب
    boolean existsByEmail(String email);
}