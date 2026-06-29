package com.market.ecommerce.dto;

import java.util.List;

import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    String totalAmount,
    String status,
    Long shippingAddressId,
    AddressResponse shippingAddress,
    String customerName,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) { }
