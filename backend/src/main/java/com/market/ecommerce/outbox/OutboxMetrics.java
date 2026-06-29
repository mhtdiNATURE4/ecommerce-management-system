package com.market.ecommerce.outbox;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

import com.market.ecommerce.repository.OutboxEventRepository;

import jakarta.annotation.PostConstruct;
import java.time.Duration;

@Component
public class OutboxMetrics {
    private final MeterRegistry registry;
    private final OutboxEventRepository outboxRepo;

    private Timer processingTimer; // fallback/global
    private DistributionSummary lagSummary; // fallback/global

    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository outboxRepo) {
        this.registry = registry;
        this.outboxRepo = outboxRepo;
    }

    @PostConstruct
    public void init() {
        processingTimer = registry.timer("outbox.processing.duration");
        lagSummary = DistributionSummary.builder("outbox.processing.lag_ms").register(registry);

        // gauges for counts
        registry.gauge("outbox.pending.count", outboxRepo, r -> r.findByStatusOrderByCreatedAtAsc(com.market.ecommerce.entity.OutboxEvent.Status.PENDING, org.springframework.data.domain.PageRequest.of(0,1)).size());
        registry.gauge("outbox.processing.count", outboxRepo, r -> r.findByStatusOrderByCreatedAtAsc(com.market.ecommerce.entity.OutboxEvent.Status.PROCESSING, org.springframework.data.domain.PageRequest.of(0,1)).size());
        registry.gauge("outbox.retry.max", outboxRepo, r -> { Integer max = r.findAll().stream().map(e -> e.getRetryCount() == null ? 0 : e.getRetryCount()).max(Integer::compareTo).orElse(0); return max.doubleValue(); });
    }

    public void recordProcessingDuration(String eventType, long millis) {
        // labeled timer per event type
        registry.timer("outbox.processing.duration", "event_type", eventType).record(Duration.ofMillis(millis));
        // global fallback
        processingTimer.record(Duration.ofMillis(millis));
    }

    public void recordProcessingLag(String eventType, long millis) {
        registry.summary("outbox.processing.lag_ms", "event_type", eventType).record(millis);
        lagSummary.record(millis);
    }

    public void incrementFailure(String eventType) {
        registry.counter("outbox.process.failures", "event_type", eventType).increment();
    }

    public void incrementDeadLetter(String eventType) {
        registry.counter("outbox.deadletter.count", "event_type", eventType).increment();
    }

    public void incrementRetry(String eventType) {
        registry.counter("outbox.retry.count", "event_type", eventType).increment();
    }
}
