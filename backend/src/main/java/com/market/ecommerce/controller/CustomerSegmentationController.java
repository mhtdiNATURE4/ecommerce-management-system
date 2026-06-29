package com.market.ecommerce.controller;

import com.market.ecommerce.dto.CustomerSegmentResponse;
import com.market.ecommerce.service.CustomerSegmentationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class CustomerSegmentationController {

    private final CustomerSegmentationService service;

    public CustomerSegmentationController(CustomerSegmentationService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/segments")
    public ResponseEntity<List<CustomerSegmentResponse>> getSegments() {
        return ResponseEntity.ok(service.segmentCustomers());
    }
}