package com.market.ecommerce.event;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.time.Instant;
import java.util.Optional;

/**
 * Deprecated legacy Spring event listener for payments.
 *
 * This listener performs domain state changes directly and has been superseded
 * by the DB-backed Outbox processor. It is disabled by default via
 * `feature.payment.legacy-listener.enabled=false` to avoid split-brain writes.
 *
 * Keep only for controlled testing or rollback scenarios.
 */
@Deprecated
@Component
@ConditionalOnProperty(prefix = "feature.payment.legacy-listener", name = "enabled", havingValue = "true", matchIfMissing = false)
public class PaymentEventListener {
    private static final Logger log = LoggerFactory.getLogger(PaymentEventListener.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;

    public PaymentEventListener(PaymentRepository paymentRepository, OrderRepository orderRepository) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        log.warn("Legacy PaymentEventListener bean created — feature.payment.legacy-listener.enabled=true. This listener is deprecated and may cause duplicate domain writes. Disable in production.");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentInitiated(PaymentInitiatedEvent event) {
        try {
            Optional<Payment> opt = paymentRepository.findById(event.getPaymentId());
            if (opt.isEmpty()) {
                log.warn("Payment not found for PaymentInitiatedEvent: {}", event.getPaymentId());
                return;
            }

            Payment payment = opt.get();
            // Simulate external gateway call here (in production, call Stripe/PayPal)
            // For now assume success
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setTransactionId("SIMULATED_" + Instant.now().toEpochMilli());
            paymentRepository.save(payment);

            Long orderId = payment.getOrder().getId();
            Order locked = orderRepository.findByIdForUpdate(orderId).orElseThrow();
            if (locked.getStatus() != OrderStatus.COMPLETED && locked.getStatus() != OrderStatus.CANCELLED) {
                locked.setStatus(OrderStatus.PROCESSING);
                orderRepository.save(locked);
            }

        } catch (Exception ex) {
            log.error("Failed to process PaymentInitiatedEvent {}", event.getPaymentId(), ex);
        }
    }
}
