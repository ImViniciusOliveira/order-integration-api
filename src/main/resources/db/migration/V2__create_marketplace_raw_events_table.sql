CREATE TABLE marketplace_raw_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    shop_id VARCHAR(100) NOT NULL,
    order_sn VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_raw_events_platform ON marketplace_raw_events (platform);
CREATE INDEX idx_raw_events_order_sn ON marketplace_raw_events (order_sn);
CREATE INDEX idx_raw_events_shop_id ON marketplace_raw_events (shop_id);
CREATE INDEX idx_raw_events_event_type ON marketplace_raw_events (event_type);
CREATE INDEX idx_raw_events_jsonb ON marketplace_raw_events USING GIN (payload_json);
