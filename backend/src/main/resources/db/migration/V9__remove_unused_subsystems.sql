-- Flyway migration: remove payment, outbox, and scheduling tables that are no longer used

DROP TABLE IF EXISTS outbox_event;
DROP TABLE IF EXISTS shedlock;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS report_schedule;
