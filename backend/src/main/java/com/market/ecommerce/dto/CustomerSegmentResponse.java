package com.market.ecommerce.dto;

import java.math.BigDecimal;

public record CustomerSegmentResponse(
        Long userId,
        String name,
        String email,
        BigDecimal totalSpent,
        String segment
) {}