CREATE TABLE orders_master (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_sn VARCHAR(50) NOT NULL UNIQUE,
    shop_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    tracking_no VARCHAR(100),
    estimated_shipping_fee DECIMAL(15, 4),
    escrow_amount DECIMAL(15, 4),
    shipping_fee_borne_by_seller DECIMAL(15, 4),
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_master_shop_status ON orders_master (shop_id, status);
CREATE INDEX idx_orders_master_status ON orders_master (status);
CREATE INDEX idx_orders_master_reconciled ON orders_master (reconciled);
CREATE INDEX idx_orders_master_created_at ON orders_master (created_at);
CREATE INDEX idx_orders_master_metadata ON orders_master USING GIN (metadata);
