package com.market.ecommerce.repository;

import com.market.ecommerce.entity.OutboxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    // Fetch pending events in order; caller provides a Pageable to control N
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.Status status, Pageable pageable);

    @Query("SELECT o.id FROM OutboxEvent o WHERE ( (o.status = com.market.ecommerce.entity.OutboxEvent.Status.PENDING OR (o.status = com.market.ecommerce.entity.OutboxEvent.Status.PROCESSING AND o.processedAt < :stale)) AND (o.nextRetryAt IS NULL OR o.nextRetryAt <= :now) ) ORDER BY o.createdAt ASC")
    List<Long> findCandidateIds(@Param("stale") OffsetDateTime stale, @Param("now") OffsetDateTime now, org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "UPDATE outbox_event SET status = :newStatus, processed_at = :processedAt WHERE id = :id AND status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= :now)", nativeQuery = true)
    int claimPending(@Param("id") Long id, @Param("newStatus") String newStatus, @Param("processedAt") OffsetDateTime processedAt, @Param("now") OffsetDateTime now);

    @Modifying
    @Transactional
    @Query(value = "UPDATE outbox_event SET status = :newStatus, processed_at = :processedAt WHERE id = :id AND status = 'PROCESSING' AND processed_at < :stale AND (next_retry_at IS NULL OR next_retry_at <= :now)", nativeQuery = true)
    int claimStaleProcessing(@Param("id") Long id, @Param("newStatus") String newStatus, @Param("processedAt") OffsetDateTime processedAt, @Param("stale") OffsetDateTime stale, @Param("now") OffsetDateTime now);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.status = :status, o.processedAt = :processedAt, o.retryCount = :retryCount WHERE o.id = :id")
    int updateStatus(@Param("id") Long id,
                     @Param("status") OutboxEvent.Status status,
                     @Param("processedAt") OffsetDateTime processedAt,
                     @Param("retryCount") Integer retryCount);

    @Modifying
    @Transactional
    @Query("UPDATE OutboxEvent o SET o.status = :status, o.processedAt = :processedAt, o.retryCount = :retryCount, o.nextRetryAt = :nextRetryAt WHERE o.id = :id")
    int updateStatusWithNextRetry(@Param("id") Long id,
                                  @Param("status") OutboxEvent.Status status,
                                  @Param("processedAt") OffsetDateTime processedAt,
                                  @Param("retryCount") Integer retryCount,
                                  @Param("nextRetryAt") OffsetDateTime nextRetryAt);
}
