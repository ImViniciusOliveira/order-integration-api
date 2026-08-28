CREATE TABLE shopee_raw_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    shop_id BIGINT NOT NULL,
    order_sn VARCHAR(50),
    push_code INT NOT NULL,
    payload_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_raw_events_order_sn ON shopee_raw_events (order_sn);
CREATE INDEX idx_raw_events_shop_id ON shopee_raw_events (shop_id);
CREATE INDEX idx_raw_events_push_code ON shopee_raw_events (push_code);
CREATE INDEX idx_raw_events_jsonb ON shopee_raw_events USING GIN (payload_json);
