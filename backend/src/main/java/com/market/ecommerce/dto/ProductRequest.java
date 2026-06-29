package com.market.ecommerce.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "اسم المنتج مطلوب")
        String name,

        String description,

        @NotNull(message = "السعر مطلوب")
        @Positive(message = "السعر يجب أن يكون أكبر من صفر")
        BigDecimal price,

        @NotNull(message = "المخزون مطلوب")
        @Min(value = 0, message = "المخزون لا يمكن أن يكون سالباً")
        Integer stock,

        String imageUrl,

        @NotNull(message = "القسم مطلوب")
        Long categoryId
) {}