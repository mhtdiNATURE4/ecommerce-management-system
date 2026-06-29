package com.market.ecommerce.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.ecommerce.config.OutboxProperties;
import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentMethod;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

public class OutboxMetricsTest {

    private OutboxEventRepository outboxRepo;
    private PaymentRepository paymentRepository;
    private OrderRepository orderRepository;
    private OutboxEventProcessor processor;
    private SimpleMeterRegistry registry;

    @BeforeEach
    void setup() {
        outboxRepo = mock(OutboxEventRepository.class);
        paymentRepository = mock(PaymentRepository.class);
        orderRepository = mock(OrderRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        var outboxMetrics = mock(com.market.ecommerce.outbox.OutboxMetrics.class);
        OutboxProperties props = new OutboxProperties();
        registry = new SimpleMeterRegistry();
        @SuppressWarnings("unchecked")
        ObjectProvider<com.market.ecommerce.service.NotificationService> notif = mock(ObjectProvider.class);

        processor = new OutboxEventProcessor(outboxRepo, paymentRepository, orderRepository, objectMapper, outboxMetrics, props, registry, notif);
    }

    private OutboxEvent makePaymentEvent(long id, long paymentId) {
        String payload = "{\"paymentId\":" + paymentId + "}";
        return OutboxEvent.builder()
                .id(id)
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
    void processedCounter_increments_on_successful_handle() {
        OutboxEvent ev = makePaymentEvent(1L, 100L);
        when(outboxRepo.findCandidateIds(any(), any(), any())).thenReturn(List.of(1L));
        when(outboxRepo.claimPending(any(), any(), any(), any())).thenReturn(1);
        when(outboxRepo.findById(1L)).thenReturn(Optional.of(ev));
        when(paymentRepository.findById(100L)).thenReturn(Optional.of(new Payment()));
        // use Mockito matchers consistently: anyInt() can't be concatenated into a string literal
        when(paymentRepository.finalizePaymentIfPending(eq(100L), anyString())).thenReturn(1);
        when(orderRepository.markOrderProcessingIfNotTerminal(any())).thenReturn(1);

        processor.processBatch(10);

        assertThat(registry.counter("outbox_event_processed_total").count()).isEqualTo(1.0);
        assertThat(registry.counter("outbox_event_failed_total").count()).isEqualTo(0.0);
    }

    @Test
    void failedCounter_increments_on_exception() {
        OutboxEvent ev = makePaymentEvent(2L, 200L);
        when(outboxRepo.findCandidateIds(any(), any(), any())).thenReturn(List.of(2L));
        when(outboxRepo.claimPending(any(), any(), any(), any())).thenReturn(1);
        when(outboxRepo.findById(2L)).thenReturn(Optional.of(ev));
        // simulate findById throwing to cause failure inside handler
        when(paymentRepository.findById(200L)).thenThrow(new RuntimeException("db fail"));

        processor.processBatch(10);

        assertThat(registry.counter("outbox_event_processed_total").count()).isEqualTo(1.0);
        assertThat(registry.counter("outbox_event_failed_total").count()).isEqualTo(1.0);
    }

    @Test
    void noopCounter_increments_when_conditional_updates_return_zero() {
        OutboxEvent ev = makePaymentEvent(3L, 300L);
        when(outboxRepo.findCandidateIds(any(), any(), any())).thenReturn(List.of(3L));
        when(outboxRepo.claimPending(any(), any(), any(), any())).thenReturn(1);
        when(outboxRepo.findById(3L)).thenReturn(Optional.of(ev));

        // return a valid Payment object so handler proceeds
        Payment payment = new Payment();
        payment.setId(300L);
        Order order = new Order();
        order.setId(400L);
        order.setStatus(OrderStatus.CREATED);
        payment.setOrder(order);
        payment.setMethod(PaymentMethod.CREDIT_CARD);
        payment.setAmount(java.math.BigDecimal.ONE);
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findById(300L)).thenReturn(Optional.of(payment));
        // simulate finalize returning 0 -> noop
        when(paymentRepository.finalizePaymentIfPending(eq(300L), anyString())).thenReturn(0);
        // simulate order update returning 0 as well
        when(orderRepository.markOrderProcessingIfNotTerminal(400L)).thenReturn(0);

        processor.processBatch(10);

        assertThat(registry.counter("outbox_event_processed_total").count()).isEqualTo(1.0);
        // noop should have been incremented for payment and for order (two increments)
        assertThat(registry.counter("outbox_event_noop_total").count()).isEqualTo(2.0);
    }
}
