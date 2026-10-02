CREATE TABLE inventory_reservations (
    reservation_id VARCHAR(128) PRIMARY KEY,
    order_id       VARCHAR(64) NOT NULL UNIQUE,
    status         VARCHAR(32) NOT NULL,
    version        BIGINT NOT NULL DEFAULT 0,
    items_json     TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at     TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_inv_res_order_id ON inventory_reservations(order_id);
