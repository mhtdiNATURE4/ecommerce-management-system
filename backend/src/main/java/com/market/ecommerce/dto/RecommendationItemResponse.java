package com.market.ecommerce.dto;

import java.math.BigDecimal;

public record RecommendationItemResponse(
        Long productId,
        String productName,
        BigDecimal score
) {}