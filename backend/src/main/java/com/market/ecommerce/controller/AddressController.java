package com.market.ecommerce.controller;

import com.market.ecommerce.dto.AddressRequest;
import com.market.ecommerce.dto.AddressResponse;
import com.market.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@Valid @RequestBody AddressRequest request) {
        AddressResponse res = addressService.addAddress(request);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(res.id())
                .toUri();
        return ResponseEntity.created(location).body(res);
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getUserAddresses() {
        return ResponseEntity.ok(addressService.getUserAddressesDto());
    }
}