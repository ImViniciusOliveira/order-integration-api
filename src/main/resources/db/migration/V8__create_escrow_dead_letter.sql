CREATE TABLE escrow_dead_letter (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    order_sn VARCHAR(100) NOT NULL,
    reason VARCHAR(100) NOT NULL,
    attempts BIGINT NOT NULL,
    failed_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_escrow_dead_letter_platform_order_sn UNIQUE (platform, order_sn)
);

CREATE INDEX idx_escrow_dead_letter_failed_at
    ON escrow_dead_letter (failed_at);
