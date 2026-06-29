package com.market.ecommerce.outbox;

import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentMethod;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OutboxMetricsSpringIT {

    @Autowired
    OutboxEventProcessor processor;

    @Autowired
    MeterRegistry registry;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void clean() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    private OutboxEvent makeEvent(long id, long paymentId) {
        String payload = "{\"paymentId\":" + paymentId + "}";
        return OutboxEvent.builder()
                .aggregateType(OutboxEvent.AggregateType.PAYMENT)
                .aggregateId(paymentId)
                .eventType("PAYMENT_INITIATED")
                .payload(payload)
                .status(OutboxEvent.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .retryCount(0)
                .build();
    }

    @Test
    void successfulProcessing_incrementsProcessedCounter() {
        double before = registry.counter("outbox_event_processed_total").count();

        Order order = Order.builder().status(OrderStatus.CREATED).totalAmount(java.math.BigDecimal.ONE).build();
        order = orderRepository.save(order);

        Payment payment = Payment.builder().order(order).amount(java.math.BigDecimal.ONE).method(PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING).build();
        payment = paymentRepository.save(payment);

        OutboxEvent ev = makeEvent(1L, payment.getId());
        ev = outboxEventRepository.save(ev);

        processor.runOnce();

        double after = registry.counter("outbox_event_processed_total").count();
        assertThat(after - before).isEqualTo(1.0);
    }

    @Test
    void noopProcessing_incrementsNoopCounter() {
        double before = registry.counter("outbox_event_noop_total").count();

        Order order = Order.builder().status(OrderStatus.PROCESSING).totalAmount(java.math.BigDecimal.ONE).build();
        order = orderRepository.save(order);

        Payment payment = Payment.builder().order(order).amount(java.math.BigDecimal.ONE).method(PaymentMethod.CREDIT_CARD).status(PaymentStatus.COMPLETED).build();
        payment = paymentRepository.save(payment);

        OutboxEvent ev = makeEvent(2L, payment.getId());
        ev = outboxEventRepository.save(ev);

        processor.runOnce();

        double after = registry.counter("outbox_event_noop_total").count();
        assertThat(after - before).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void failureProcessing_incrementsFailedCounter() {
        double before = registry.counter("outbox_event_failed_total").count();

        // create an outbox event with invalid payload to force handler exception
        OutboxEvent ev = OutboxEvent.builder()
                .aggregateType(OutboxEvent.AggregateType.PAYMENT)
                .aggregateId(null)
                .eventType("PAYMENT_INITIATED")
                .payload("{}")
                .status(OutboxEvent.Status.PENDING)
                .createdAt(OffsetDateTime.now())
                .retryCount(0)
                .build();
        ev = outboxEventRepository.save(ev);

        processor.runOnce();

        double after = registry.counter("outbox_event_failed_total").count();
        assertThat(after - before).isEqualTo(1.0);
    }
}
