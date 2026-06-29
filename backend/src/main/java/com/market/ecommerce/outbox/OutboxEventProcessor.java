package com.market.ecommerce.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.market.ecommerce.entity.OutboxEvent;
import com.market.ecommerce.entity.Order;
import com.market.ecommerce.entity.OrderStatus;
import com.market.ecommerce.entity.Payment;
import com.market.ecommerce.entity.PaymentStatus;
import com.market.ecommerce.repository.OutboxEventRepository;
import com.market.ecommerce.repository.OrderRepository;
import com.market.ecommerce.repository.PaymentRepository;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Optional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxEventProcessor {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventProcessor.class);

    private final OutboxEventRepository outboxRepo;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;
    private final com.market.ecommerce.outbox.OutboxMetrics outboxMetrics;
    private final Counter outboxProcessedCounter;
    private final Counter outboxFailedCounter;
    private final Counter outboxNoopCounter;
    private final com.market.ecommerce.service.NotificationService notificationService;
    private final int claimSize;
    private final long reclaimTtlSeconds;
    private final int maxRetries;
    private final long retryBackoffSeconds;
    private final long baseBackoffSeconds;
    private final long maxBackoffSeconds;
    private final long jitterSeconds;

    public OutboxEventProcessor(OutboxEventRepository outboxRepo,
                                PaymentRepository paymentRepository,
                                OrderRepository orderRepository,
                                ObjectMapper objectMapper,
                                com.market.ecommerce.outbox.OutboxMetrics outboxMetrics,
                                com.market.ecommerce.config.OutboxProperties props,
                                MeterRegistry meterRegistry,
                                ObjectProvider<com.market.ecommerce.service.NotificationService> notificationServiceProvider) {
        this.outboxRepo = outboxRepo;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
        this.outboxMetrics = outboxMetrics;
        this.claimSize = props.getBatchSize();
        this.reclaimTtlSeconds = props.getReclaimTtlSeconds();
        this.maxRetries = props.getMaxRetries();
        this.retryBackoffSeconds = props.getRetryBackoffSeconds();
        this.baseBackoffSeconds = props.getBaseBackoffSeconds();
        this.maxBackoffSeconds = props.getMaxBackoffSeconds();
        this.jitterSeconds = props.getJitterSeconds();
        this.notificationService = notificationServiceProvider.getIfAvailable();
        this.outboxProcessedCounter = Counter.builder("outbox_event_processed_total").register(meterRegistry);
        this.outboxFailedCounter = Counter.builder("outbox_event_failed_total").register(meterRegistry);
        this.outboxNoopCounter = Counter.builder("outbox_event_noop_total").register(meterRegistry);
    }
    @Scheduled(fixedDelayString = "${outbox.processor.delay:5000}")
    public void runOnce() {
        try {
            processBatch(this.claimSize);
        } catch (Exception ex) {
            log.error("Outbox processor failed", ex);
        }
    }

    @Transactional
    protected void processBatch(int limit) {
        // Claim TTL for processing stale PROCESSING events (configurable)
        Duration staleTtl = Duration.ofSeconds(this.reclaimTtlSeconds);
        OffsetDateTime staleThreshold = OffsetDateTime.now().minus(staleTtl);

        org.springframework.data.domain.Pageable pg = org.springframework.data.domain.PageRequest.of(0, limit);
        OffsetDateTime now = OffsetDateTime.now();
        List<Long> candidates = outboxRepo.findCandidateIds(staleThreshold, now, pg);
        if (candidates == null || candidates.isEmpty()) return;

        for (Long id : candidates) {
            OffsetDateTime claimTime = OffsetDateTime.now();
            int claimed = outboxRepo.claimPending(id, OutboxEvent.Status.PROCESSING.name(), claimTime, OffsetDateTime.now());
            if (claimed == 0) {
                // try to claim stale PROCESSING that may have been abandoned
                claimed = outboxRepo.claimStaleProcessing(id, OutboxEvent.Status.PROCESSING.name(), claimTime, staleThreshold, OffsetDateTime.now());
            }
            if (claimed == 0) continue; // someone else claimed it

            Optional<OutboxEvent> opt = outboxRepo.findById(id);
            if (opt.isEmpty()) continue;
            OutboxEvent ev = opt.get();

            long start = System.currentTimeMillis();
            try {
                // Observability: increment processed counter for each handling attempt
                this.outboxProcessedCounter.increment();
                handleEvent(ev);
                long dur = System.currentTimeMillis() - start;
                outboxMetrics.recordProcessingDuration(ev.getEventType(), dur);
                // record lag
                if (ev.getCreatedAt() != null) {
                    long lagMs = java.time.Duration.between(ev.getCreatedAt(), OffsetDateTime.now()).toMillis();
                    outboxMetrics.recordProcessingLag(ev.getEventType(), lagMs);
                }
                outboxRepo.updateStatus(ev.getId(), OutboxEvent.Status.SENT, OffsetDateTime.now(), ev.getRetryCount());
            } catch (Exception ex) {
                long dur = System.currentTimeMillis() - start;
                outboxMetrics.recordProcessingDuration(ev.getEventType(), dur);
                // Observability: increment failed counter on exceptions
                this.outboxFailedCounter.increment();
                int newRetry = (ev.getRetryCount() == null ? 1 : ev.getRetryCount() + 1);
                if (newRetry >= this.maxRetries) {
                    // mark as dead
                    outboxRepo.updateStatus(ev.getId(), OutboxEvent.Status.DEAD, OffsetDateTime.now(), newRetry);
                    outboxMetrics.incrementDeadLetter(ev.getEventType());
                    log.warn("Outbox event marked DEAD: eventId={} eventType={} aggregateId={} retries={}", ev.getId(), ev.getEventType(), ev.getAggregateId(), newRetry);
                    try {
                        if (this.notificationService != null) {
                            this.notificationService.notifyDeadLetter(ev.getEventType(), ev.getAggregateId(), newRetry, ev.getId());
                        }
                    } catch (Exception nEx) {
                        log.warn("NotificationService failed for dead-letter event {}: {}", ev.getId(), nEx.toString());
                    }
                } else {
                    // exponential backoff with jitter
                    double exp = Math.pow(2.0, (double)(newRetry - 1));
                    double rawBackoff = ((double) this.baseBackoffSeconds) * exp;
                    long backoffSeconds = Math.min(this.maxBackoffSeconds, (long) rawBackoff);
                    long jitter = (this.jitterSeconds > 0) ? ThreadLocalRandom.current().nextLong(0, this.jitterSeconds + 1) : 0L;
                    OffsetDateTime nextRetry = OffsetDateTime.now().plusSeconds(backoffSeconds + jitter);
                    outboxRepo.updateStatusWithNextRetry(ev.getId(), OutboxEvent.Status.PENDING, OffsetDateTime.now(), newRetry, nextRetry);
                    outboxMetrics.incrementRetry(ev.getEventType());
                }
                // increment failure metric per event type
                    outboxMetrics.incrementFailure(ev.getEventType());
                log.warn("Failed processing outbox event {} (type={}), retry={}, error={}", ev.getId(), ev.getEventType(), newRetry, ex.toString());
            }
        }
    }

    private void handleEvent(OutboxEvent ev) throws Exception {
        String type = ev.getEventType();
        JsonNode payload = objectMapper.readTree(ev.getPayload() == null ? "{}" : ev.getPayload());

        switch (type) {
            case "ORDER_CREATED" -> handleOrderCreated(ev, payload);
            case "PAYMENT_INITIATED" -> handlePaymentInitiated(ev, payload);
            default -> log.warn("Unknown outbox event type: {}", type);
        }
    }

    private void handleOrderCreated(OutboxEvent ev, JsonNode payload) {
        // For now, stub sending email — keep idempotent by logging
        Long orderId = payload.has("orderId") && !payload.get("orderId").isNull() ? payload.get("orderId").asLong() : null;
        log.info("Outbox ORDER_CREATED for orderId={} outboxId={}", orderId, ev.getId());
    }

    private void handlePaymentInitiated(OutboxEvent ev, JsonNode payload) {
        Long paymentId = payload.has("paymentId") && !payload.get("paymentId").isNull() ? payload.get("paymentId").asLong() : null;
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId missing in payload");
        }

        // Read payment to obtain order id (we avoid modifying entities; updates use conditional SQL)
        Payment payment = paymentRepository.findById(paymentId).orElseThrow(() -> new IllegalStateException("Payment not found: " + paymentId));
        Long orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;

        // Simulate external gateway success and finalize payment atomically
        String txId = "SIMULATED_OUTBOX_" + System.currentTimeMillis();
        int updated = paymentRepository.finalizePaymentIfPending(paymentId, txId);
        if (updated == 0) {
            this.outboxNoopCounter.increment();
            log.info("Payment {} was not finalized because it was not PENDING (treated as already applied)", paymentId);
        } else {
            log.info("Finalized Payment {} via conditional update (rows={})", paymentId, updated);
        }

        // Transition order to PROCESSING using an atomic conditional update; treat 0 rows as already-applied
        if (orderId != null) {
            int orderUpdated = orderRepository.markOrderProcessingIfNotTerminal(orderId);
            if (orderUpdated == 0) {
                this.outboxNoopCounter.increment();
                log.info("Order {} not transitioned to PROCESSING because it is already in a terminal or processing state", orderId);
            } else {
                log.info("Order {} transitioned to PROCESSING via conditional update (rows={})", orderId, orderUpdated);
            }
        }

        log.info("Processed PAYMENT_INITIATED for paymentId={}, outboxId={}", paymentId, ev.getId());
    }
}
