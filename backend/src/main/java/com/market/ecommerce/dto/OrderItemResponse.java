package com.market.ecommerce.dto;

public record OrderItemResponse(Long id, Long productId, String productName, int quantity, String price) { }
