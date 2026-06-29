package com.market.ecommerce.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "outbox.processor")
@Validated
public class OutboxProperties {

    @Positive(message = "batch-size must be > 0")
    private int batchSize = 50;

    @Positive(message = "reclaim-ttl-seconds must be > 0")
    private long reclaimTtlSeconds = 60;

    @Min(value = 0, message = "max-retries must be >= 0")
    private int maxRetries = 5;

    @Min(value = 0, message = "retry-backoff-seconds must be >= 0")
    private long retryBackoffSeconds = 60;

    @Min(value = 0, message = "base-backoff-seconds must be >= 0")
    private long baseBackoffSeconds = 60;

    @Min(value = 0, message = "max-backoff-seconds must be >= 0")
    private long maxBackoffSeconds = 3600;

    @Min(value = 0, message = "jitter-seconds must be >= 0")
    private long jitterSeconds = 30;

    @PostConstruct
    public void validate() {
        if (maxBackoffSeconds < baseBackoffSeconds) {
            throw new IllegalStateException("outbox.processor.max-backoff-seconds must be >= outbox.processor.base-backoff-seconds");
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getReclaimTtlSeconds() {
        return reclaimTtlSeconds;
    }

    public void setReclaimTtlSeconds(long reclaimTtlSeconds) {
        this.reclaimTtlSeconds = reclaimTtlSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffSeconds() {
        return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(long retryBackoffSeconds) {
        this.retryBackoffSeconds = retryBackoffSeconds;
    }

    public long getBaseBackoffSeconds() {
        return baseBackoffSeconds;
    }

    public void setBaseBackoffSeconds(long baseBackoffSeconds) {
        this.baseBackoffSeconds = baseBackoffSeconds;
    }

    public long getMaxBackoffSeconds() {
        return maxBackoffSeconds;
    }

    public void setMaxBackoffSeconds(long maxBackoffSeconds) {
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    public long getJitterSeconds() {
        return jitterSeconds;
    }

    public void setJitterSeconds(long jitterSeconds) {
        this.jitterSeconds = jitterSeconds;
    }
}
