-- Inserção do canal oficial Mercado Livre na tabela de canais de marketplace
INSERT INTO marketplace_channels (code, name, active) VALUES
    ('MERCADOLIVRE', 'Mercado Livre', true)
ON CONFLICT (code) DO NOTHING;
