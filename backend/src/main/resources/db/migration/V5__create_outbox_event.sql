-- Flyway migration: create outbox_event table

CREATE TABLE outbox_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id BIGINT NULL,
  event_type VARCHAR(100) NOT NULL,
  payload TEXT,
  status VARCHAR(20) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_at TIMESTAMP NULL
);

CREATE INDEX idx_outbox_event_status_created
ON outbox_event (status, created_at);