package com.market.ecommerce.controller;

import com.market.ecommerce.dto.PaymentRequest;
import com.market.ecommerce.dto.PaymentResponse;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.security.PaymentSecurity;
import com.market.ecommerce.service.OrderService;
import com.market.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentSecurity paymentSecurity;

    public PaymentController(PaymentService paymentService, OrderService orderService, PaymentSecurity paymentSecurity) {
        this.paymentService = paymentService;
        this.orderService = orderService;
        this.paymentSecurity = paymentSecurity;
    }

    @PreAuthorize("@paymentSecurity.canPayOrder(#request.orderId)")
    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                      @Valid @RequestBody PaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Order order = orderService.getOrderByIdWithUser(request.orderId());

        Payment payment = paymentService.processPayment(request, order);

        PaymentResponse res = new PaymentResponse(payment.getId(), order.getId(), payment.getStatus().name(), payment.getAmount().toString());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(payment.getId())
            .toUri();
        return ResponseEntity.created(location).body(res);
    }
}
