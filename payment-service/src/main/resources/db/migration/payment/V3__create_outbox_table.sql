-- V3__create_outbox_table.sql
CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_event_published_at ON outbox_event(published_at);
CREATE INDEX idx_outbox_event_retry_count ON outbox_event(retry_count);
CREATE INDEX idx_outbox_event_aggregate ON outbox_event(aggregate_type, aggregate_id);