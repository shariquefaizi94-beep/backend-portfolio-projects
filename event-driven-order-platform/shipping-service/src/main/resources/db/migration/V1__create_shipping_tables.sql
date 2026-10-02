CREATE TABLE shipments (
    shipment_id     VARCHAR(128) PRIMARY KEY,
    order_id        VARCHAR(64) NOT NULL UNIQUE,
    tracking_number VARCHAR(128) NOT NULL,
    carrier         VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_shipment_order_id ON shipments(order_id);
