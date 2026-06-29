package com.market.ecommerce.service;

import com.market.ecommerce.dto.AddressRequest;
import com.market.ecommerce.dto.AddressResponse;
import com.market.ecommerce.entity.Address;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.exception.ResourceNotFoundException;
import com.market.ecommerce.repository.AddressRepository;
import com.market.ecommerce.repository.UserRepository;
import com.market.ecommerce.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public AddressService(
            AddressRepository addressRepository,
            UserRepository userRepository
    ) {
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AddressResponse addAddress(AddressRequest request) {

        String email = SecurityUtils.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("المستخدم غير موجود"));

        Address address = Address.builder()
                .street(request.street().trim())
                .city(request.city().trim())
                .country(request.country().trim())
                .zipCode(
                        request.zipCode() == null
                                ? null
                                : request.zipCode().trim()
                )
                .user(user)
                .build();

        return toDto(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddressesDto() {

        String email = SecurityUtils.getCurrentUserEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("المستخدم غير موجود"));

        return addressRepository.findByUserId(user.getId()).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private AddressResponse toDto(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getStreet(),
                address.getCity(),
                address.getCountry(),
                address.getZipCode()
        );
    }
}