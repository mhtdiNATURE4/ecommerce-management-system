package com.market.ecommerce.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartQuantityRequest(
        @Min(1) Integer quantity
) {}
