-- V2__create_reservations_table.sql
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    order_item_id BIGINT NOT NULL UNIQUE,
    variant_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RESERVED', 'RELEASED', 'EXPIRED')),
    released_at TIMESTAMP
);

CREATE INDEX idx_reservations_variant_id ON reservations(variant_id);
CREATE INDEX idx_reservations_order_item_id ON reservations(order_item_id);
CREATE INDEX idx_reservations_status ON reservations(status);