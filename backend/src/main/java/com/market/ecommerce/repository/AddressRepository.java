package com.market.ecommerce.repository;

import com.market.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // جلب جميع العناوين الخاصة بمستخدم معين
    List<Address> findByUserId(Long userId);
}