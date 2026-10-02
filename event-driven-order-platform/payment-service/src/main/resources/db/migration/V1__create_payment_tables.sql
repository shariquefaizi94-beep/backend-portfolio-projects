CREATE TABLE payments (
    transaction_id VARCHAR(128) PRIMARY KEY,
    order_id       VARCHAR(64) NOT NULL UNIQUE,
    customer_id    VARCHAR(64) NOT NULL,
    amount         DECIMAL(12,2) NOT NULL,
    currency       VARCHAR(3) DEFAULT 'USD',
    status         VARCHAR(32) NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    failure_reason VARCHAR(1024),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_order_id ON payments(order_id);
CREATE INDEX idx_payment_status ON payments(status);
