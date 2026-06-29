package com.market.ecommerce.dto;

import com.market.ecommerce.entity.UserRole;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        LocalDateTime createdAt
) {}