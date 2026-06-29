package com.market.ecommerce.dto;

public record AddressResponse(
        Long id,
        String street,
        String city,
        String country,
        String zipCode
) {
}
