package com.market.ecommerce.service;

import com.market.ecommerce.dto.UserResponse;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.UserRepository;
import com.market.ecommerce.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // جلب معلومات حساب المستخدم الحالي المسجل بالنظام
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfileDto() {
        String email = SecurityUtils.getCurrentUserEmail();
        return userRepository.findByEmail(email)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("لم يتم العثور على حساب مستخدم بهذا البريد"));
    }

    // استدعاء جميع المستخدمين (لصلاحية لوحة الإدارة فقط)
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsersDto() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserResponse> getAllUsersPaged(org.springframework.data.domain.Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDto);
    }

    private UserResponse toDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt()
        );
    }
}