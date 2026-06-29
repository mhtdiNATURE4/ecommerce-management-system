package com.market.ecommerce.controller;

import com.market.ecommerce.dto.AddToCartRequest;
import com.market.ecommerce.dto.CartItemResponse;
import com.market.ecommerce.dto.UpdateCartQuantityRequest;
import com.market.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // إضافة للسلة باستخدام مسار الجذر مباشرة
    @PostMapping
    public ResponseEntity<CartItemResponse> addToCart(@Valid @RequestBody AddToCartRequest request) {
        var created = cartService.addToCart(request);
        var dto = cartService.toDto(created);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getUserCart() {
        return ResponseEntity.ok(cartService.getUserCartDto());
    }

    @PutMapping("/{cartItemId}/quantity")
    public ResponseEntity<CartItemResponse> updateQuantity(
            @PathVariable @jakarta.validation.constraints.Positive Long cartItemId,
            @RequestParam(required = false) Integer quantity,
            @RequestBody(required = false) @Valid UpdateCartQuantityRequest body) {
        Integer resolvedQuantity = quantity;
        if (resolvedQuantity == null && body != null) {
            resolvedQuantity = body.quantity();
        }
        if (resolvedQuantity == null || resolvedQuantity < 1) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "الكمية يجب أن تكون أكبر من صفر"
            );
        }
        return ResponseEntity.ok(cartService.toDto(cartService.updateQuantity(cartItemId, resolvedQuantity)));
    }

    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeItem(@PathVariable @jakarta.validation.constraints.Positive Long cartItemId) {
        cartService.removeItem(cartItemId);
        return ResponseEntity.noContent().build();
    }
}
