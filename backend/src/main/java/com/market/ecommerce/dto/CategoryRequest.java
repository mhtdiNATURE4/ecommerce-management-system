package com.market.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank(message = "اسم القسم مطلوب")
        @Size(max = 100, message = "اسم القسم يجب ألا يتجاوز 100 حرف")
        String name
) {
}
