-- Flyway migration: add next_retry_at column to outbox_event
ALTER TABLE outbox_event
  ADD COLUMN next_retry_at TIMESTAMP NULL;

-- ensure retry_count exists (no-op if already present)
ALTER TABLE outbox_event
  MODIFY COLUMN retry_count INT NOT NULL DEFAULT 0;
