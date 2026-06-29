package com.market.ecommerce.dto;

public record AuthResponse(
        String token,
        String name,
        String role
) {}