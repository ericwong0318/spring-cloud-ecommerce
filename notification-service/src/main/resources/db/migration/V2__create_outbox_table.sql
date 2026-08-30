-- Outbox table for event publishing (transactional outbox pattern)
CREATE TABLE event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_event_outbox_published_at ON event_outbox(published_at) WHERE published_at IS NULL;
CREATE INDEX idx_event_outbox_aggregate ON event_outbox(aggregate_type, aggregate_id);