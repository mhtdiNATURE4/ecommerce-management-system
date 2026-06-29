package com.market.ecommerce.dto;

public record PaymentResponse(Long id, Long orderId, String status, String amount) { }
