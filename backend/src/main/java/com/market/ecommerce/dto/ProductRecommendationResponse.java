package com.market.ecommerce.dto;

import java.util.List;

public record ProductRecommendationResponse(
        Long productId,
        String productName,
        List<RecommendationItemResponse> recommendations
) {}