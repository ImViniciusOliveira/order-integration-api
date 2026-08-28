CREATE TABLE marketplace_channels (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_marketplace_channels_code ON marketplace_channels (code);
CREATE INDEX idx_marketplace_channels_active ON marketplace_channels (active);

-- Seed inicial de plataformas conhecidas
INSERT INTO marketplace_channels (code, name, active) VALUES
    ('SHOPEE', 'Shopee', true),
    ('TIKTOK', 'TikTok Shop', true),
    ('MERCADO_LIVRE', 'Mercado Livre', true),
    ('AMAZON', 'Amazon', true);
