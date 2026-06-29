package com.market.ecommerce.service;

import com.market.ecommerce.dto.PaymentRequest;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentMethod;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.exception.BadRequestException;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final com.market.ecommerce.event.PaymentInitiatedPublisher paymentInitiatedPublisher;

    public PaymentService(PaymentRepository paymentRepository, OrderRepository orderRepository,
                          com.market.ecommerce.event.PaymentInitiatedPublisher paymentInitiatedPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentInitiatedPublisher = paymentInitiatedPublisher;
    }

    @Transactional
    public Payment processPayment(PaymentRequest request, Order order) {
        if (order == null) throw new BadRequestException("Order not found");
        // Lock the order row to avoid concurrent payment processing for same order
        Order lockedOrder = orderRepository.findByIdForUpdate(order.getId())
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (lockedOrder.getStatus() == OrderStatus.COMPLETED) {
            throw new BadRequestException("الطلب مدفوع بالفعل");
        }
        if (lockedOrder.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("لا يمكن دفع طلب ملغي");
        }

        // Re-check payments inside the same DB lock/transaction to avoid race
        var existing = paymentRepository.findByOrder_Id(lockedOrder.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(request.amount());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("Invalid payment amount");
        }

        if (lockedOrder.getTotalAmount() == null || lockedOrder.getTotalAmount().compareTo(amount) != 0) {
            throw new BadRequestException("Payment amount does not match order total");
        }

        PaymentMethod method;
        try {
            method = PaymentMethod.valueOf(request.method());
        } catch (IllegalArgumentException ex) {
            // Accept common frontend aliases and case-insensitive values
            String m = request.method().toUpperCase().trim();
            switch (m) {
                case "CARD":
                case "CREDIT_CARD":
                    method = PaymentMethod.CREDIT_CARD;
                    break;
                case "PAYPAL":
                    method = PaymentMethod.PAYPAL;
                    break;
                case "CASH":
                case "CASH_ON_DELIVERY":
                    method = PaymentMethod.CASH_ON_DELIVERY;
                    break;
                default:
                    throw new BadRequestException("Invalid payment method");
            }
        }

        Payment payment = Payment.builder()
            .order(lockedOrder)
            .amount(amount)
            .method(method)
            .status(PaymentStatus.PENDING)
            .transactionId(null)
            .build();

        Payment saved;
        try {
            saved = paymentRepository.save(payment);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // concurrent payment created for this order; return existing
            var existingPayment = paymentRepository.findByOrder_Id(lockedOrder.getId());
            if (existingPayment.isPresent()) return existingPayment.get();
            throw ex;
        }

        // publish event to process payment after commit (external gateway call simulated by listener)
        paymentInitiatedPublisher.publish(saved.getId());

        return saved;
    }
}
