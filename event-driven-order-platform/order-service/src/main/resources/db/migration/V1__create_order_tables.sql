CREATE TABLE orders (
    id               VARCHAR(32) PRIMARY KEY,
    idempotency_key  VARCHAR(128) NOT NULL UNIQUE,
    customer_id      VARCHAR(64) NOT NULL,
    status           VARCHAR(32) NOT NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    total_amount     DECIMAL(12,2) NOT NULL,
    currency         VARCHAR(3) DEFAULT 'USD',
    transaction_id   VARCHAR(128),
    reservation_id   VARCHAR(128),
    shipment_id      VARCHAR(128),
    tracking_number  VARCHAR(128),
    failure_reason   VARCHAR(1024),
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at       TIMESTAMP WITH TIME ZONE,
    completed_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_order_customer_id ON orders(customer_id);
CREATE INDEX idx_order_status ON orders(status);

CREATE TABLE order_items (
    id       UUID PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    sku      VARCHAR(64) NOT NULL,
    quantity INT NOT NULL,
    price    DECIMAL(10,2) NOT NULL
);

CREATE INDEX idx_order_item_order_id ON order_items(order_id);

-- Transactional outbox table
CREATE TABLE outbox_events (
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id   VARCHAR(128) NOT NULL,
    event_type     VARCHAR(64) NOT NULL,
    topic          VARCHAR(128) NOT NULL,
    payload        TEXT NOT NULL,
    sent           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    sent_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_sent ON outbox_events(sent);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);
