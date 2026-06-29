package com.market.ecommerce.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull(message = "يجب تحديد عنوان الشحن")
        Long shippingAddressId
) {}