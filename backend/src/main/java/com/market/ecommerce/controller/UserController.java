package com.market.ecommerce.controller;

import com.market.ecommerce.dto.UserResponse;
import com.market.ecommerce.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(userService.getCurrentUserProfileDto());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsersDto());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all/paged")
    public ResponseEntity<org.springframework.data.domain.Page<UserResponse>> getAllUsersPaged(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsersPaged(pageable));
    }
}