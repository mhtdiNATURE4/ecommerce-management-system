package com.market.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull(message = "معرف المنتج مطلوب")
        Long productId,

        @NotNull(message = "الكمية مطلوبة")
        @Min(value = 1, message = "الكمية يجب أن تكون 1 كحد أدنى")
        Integer quantity
) {}