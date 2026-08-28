CREATE TABLE orders_master (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    shop_id VARCHAR(100) NOT NULL,
    order_sn VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    tracking_no VARCHAR(100),
    estimated_shipping_fee DECIMAL(15, 4),
    escrow_amount DECIMAL(15, 4),
    shipping_fee_borne_by_seller DECIMAL(15, 4),
    reconciled BOOLEAN NOT NULL DEFAULT FALSE,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_master_platform_order_sn UNIQUE (platform, order_sn)
);

CREATE INDEX idx_orders_master_platform_shop_status ON orders_master (platform, shop_id, status);
CREATE INDEX idx_orders_master_status ON orders_master (status);
CREATE INDEX idx_orders_master_reconciled ON orders_master (reconciled);
CREATE INDEX idx_orders_master_created_at ON orders_master (created_at);
CREATE INDEX idx_orders_master_metadata ON orders_master USING GIN (metadata);
