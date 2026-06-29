package com.market.ecommerce.dto;

import java.math.BigDecimal;

public record CustomerSegmentResponse(
        Long userId,
        String name,
        BigDecimal totalSpent,
        String segment
) {}