package com.market.ecommerce.controller;

import com.market.ecommerce.service.MarketBasketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class MarketBasketController {

    private final MarketBasketService marketBasketService;

    public MarketBasketController(MarketBasketService marketBasketService) {
        this.marketBasketService = marketBasketService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/basket")
    public ResponseEntity<Map<String, Double>> getAnalysis() {
        return ResponseEntity.ok(marketBasketService.analyze());
    }
}