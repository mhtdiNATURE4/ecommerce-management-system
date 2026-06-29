package com.market.ecommerce.controller;

import com.market.ecommerce.dto.AuthResponse;
import com.market.ecommerce.dto.LoginRequest;
import com.market.ecommerce.dto.RegisterRequest;
import com.market.ecommerce.entity.User;
import com.market.ecommerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Note: cookie-based auth removed. Token is returned in response body only.

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request);

        // لا يوجد Token عند التسجيل المبدئي (اختياري حسب منطق عملك)
        AuthResponse response = new AuthResponse(null, user.getName(), user.getRole().name());

        var location = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/user/{id}")
                .buildAndExpand(user.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse auth = authService.login(request);

        // Token is returned in response body. Clients must set `Authorization: Bearer <token>`.
        return ResponseEntity.ok(auth);
    }
}
