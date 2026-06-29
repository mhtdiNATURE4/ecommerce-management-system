package com.market.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank(message = "الشارع مطلوب")
        String street,

        @NotBlank(message = "المدينة مطلوبة")
        String city,

        @NotBlank(message = "الدولة مطلوبة")
        String country,

        @Size(max = 20, message = "الرمز البريدي يجب ألا يتجاوز 20 حرفاً")
        String zipCode
) {}