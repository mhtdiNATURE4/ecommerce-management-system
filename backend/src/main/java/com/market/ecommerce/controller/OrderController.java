package com.market.ecommerce.controller;

import com.market.ecommerce.dto.OrderResponse;
import com.market.ecommerce.dto.CheckoutRequest;
import com.market.ecommerce.security.OrderSecurity;
import com.market.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@RestController
@Validated
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSecurity orderSecurity;

    public OrderController(OrderService orderService, OrderSecurity orderSecurity) {
        this.orderService = orderService;
        this.orderSecurity = orderSecurity;
    }

    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                  @Valid @RequestBody CheckoutRequest request) {
        String effectiveIdempotencyKey = (idempotencyKey == null || idempotencyKey.isBlank())
                ? UUID.randomUUID().toString()
                : idempotencyKey.trim();
        var dto = orderService.checkoutDto(request, effectiveIdempotencyKey);
        var location = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/orders/{id}")
                .buildAndExpand(dto.id())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        return ResponseEntity.ok(orderService.getUserOrdersDto());
    }

    @GetMapping("/paged")
    public ResponseEntity<org.springframework.data.domain.Page<OrderResponse>> getUserOrdersPaged(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(orderService.getUserOrdersPaged(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@orderSecurity.isOrderOwner(#id)")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return ResponseEntity.ok(orderService.getOrderByIdDto(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrdersDto());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all/paged")
    public ResponseEntity<org.springframework.data.domain.Page<OrderResponse>> getAllOrdersPaged(org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(orderService.getAllOrdersPaged(pageable));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/process")
    public ResponseEntity<OrderResponse> processOrder(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return ResponseEntity.ok(orderService.toDto(orderService.startProcessing(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/complete")
    public ResponseEntity<OrderResponse> completeOrder(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        return ResponseEntity.ok(orderService.toDto(orderService.complete(id)));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrderByAdmin(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        orderService.cancel(id);
        return ResponseEntity.ok(orderService.toDto(orderService.getOrderById(id)));
    }

    @PreAuthorize("@orderSecurity.isOrderOwner(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelOrder(@PathVariable @jakarta.validation.constraints.Positive Long id) {
        orderService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
