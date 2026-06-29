package com.market.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull Long orderId, @NotBlank String amount, @NotBlank String method) { }
