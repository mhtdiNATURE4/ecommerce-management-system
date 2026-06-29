package com.market.ecommerce.controller;

import com.market.ecommerce.dto.ProductRecommendationResponse;
import com.market.ecommerce.service.RecommendationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<ProductRecommendationResponse>> getAllRecommendations() {
        return ResponseEntity.ok(recommendationService.buildRecommendations());
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<ProductRecommendationResponse> getProductRecommendations(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return ResponseEntity.ok(recommendationService.getRecommendationsForProduct(id));
    }
}