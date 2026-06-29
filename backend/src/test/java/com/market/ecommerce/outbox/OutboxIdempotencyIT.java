package com.market.ecommerce.outbox;

import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class OutboxIdempotencyIT {

    @Autowired
    PaymentRepository paymentRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Autowired
    OutboxEventProcessor outboxEventProcessor;

    @BeforeEach
    void cleanup() {
        outboxEventRepository.deleteAll();
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void paymentFinalizationIdempotency_repoDirect() {
        Order order = Order.builder().status(OrderStatus.CREATED).totalAmount(java.math.BigDecimal.valueOf(10)).build();
        order = orderRepository.save(order);

        Payment p = Payment.builder().order(order).amount(java.math.BigDecimal.valueOf(10)).method(com.market.ecommerce.entity.PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING).build();
        p = paymentRepository.save(p);

        String tx1 = "TX1_" + System.currentTimeMillis();
        int rows1 = paymentRepository.finalizePaymentIfPending(p.getId(), tx1);
        assertThat(rows1).isEqualTo(1);

        Payment after = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(after.getTransactionId()).isEqualTo(tx1);

        // second attempt with different TX should be a no-op
        String tx2 = "TX2_" + System.currentTimeMillis();
        int rows2 = paymentRepository.finalizePaymentIfPending(p.getId(), tx2);
        assertThat(rows2).isEqualTo(0);

        Payment after2 = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(after2.getTransactionId()).isEqualTo(tx1);
    }

    @Test
    void orderMarkProcessingIdempotency_repoDirect() {
        Order order = Order.builder().status(OrderStatus.CREATED).totalAmount(java.math.BigDecimal.valueOf(5)).build();
        order = orderRepository.save(order);

        int r1 = orderRepository.markOrderProcessingIfNotTerminal(order.getId());
        assertThat(r1).isEqualTo(1);

        Order after = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        int r2 = orderRepository.markOrderProcessingIfNotTerminal(order.getId());
        assertThat(r2).isEqualTo(0);
    }

    @Test
    void outboxReplaySafety_processorRunsOnlyOnce() throws Exception {
        Order order = Order.builder().status(OrderStatus.CREATED).totalAmount(java.math.BigDecimal.valueOf(7)).build();
        order = orderRepository.save(order);

        Payment p = Payment.builder().order(order).amount(java.math.BigDecimal.valueOf(7)).method(com.market.ecommerce.entity.PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING).build();
        p = paymentRepository.save(p);

        String payload = "{\"paymentId\":" + p.getId() + "}";
        OutboxEvent ev = OutboxEvent.builder().aggregateType(OutboxEvent.AggregateType.PAYMENT).aggregateId(p.getId()).eventType("PAYMENT_INITIATED").payload(payload).status(OutboxEvent.Status.PENDING).createdAt(OffsetDateTime.now()).retryCount(0).build();
        ev = outboxEventRepository.save(ev);

        // First run should process and finalize payment
        outboxEventProcessor.runOnce();
        Payment after = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        String txFirst = after.getTransactionId();
        assertThat(txFirst).isNotNull();

        // Second run (replay) should not change transaction id
        outboxEventProcessor.runOnce();
        Payment after2 = paymentRepository.findById(p.getId()).orElseThrow();
        assertThat(after2.getTransactionId()).isEqualTo(txFirst);

        // Outbox event should be marked SENT
        OutboxEvent stored = outboxEventRepository.findById(ev.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(OutboxEvent.Status.SENT);
    }

    @Test
    void concurrentExecution_simultaneousRepoCalls_onlyOneSucceeds() throws Exception {
        Order order = Order.builder().status(OrderStatus.CREATED).totalAmount(java.math.BigDecimal.valueOf(9)).build();
        order = orderRepository.save(order);

        Payment p = Payment.builder().order(order).amount(java.math.BigDecimal.valueOf(9)).method(com.market.ecommerce.entity.PaymentMethod.CREDIT_CARD).status(PaymentStatus.PENDING).build();
        p = paymentRepository.save(p);
        final Payment payment = p;

        ExecutorService ex = Executors.newFixedThreadPool(2);
        Callable<Integer> task = () -> paymentRepository.finalizePaymentIfPending(payment.getId(), "CONC_" + Thread.currentThread().getId());
        Future<Integer> f1 = ex.submit(task);
        Future<Integer> f2 = ex.submit(task);

        int sum = f1.get(5, TimeUnit.SECONDS) + f2.get(5, TimeUnit.SECONDS);
        // Exactly one should have succeeded
        assertThat(sum).isEqualTo(1);

        ex.shutdownNow();
    }
}
