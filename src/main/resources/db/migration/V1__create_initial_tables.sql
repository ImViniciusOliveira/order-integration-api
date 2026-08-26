CREATE TABLE pedidos_externos (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  codigo_pedido VARCHAR(100) NOT NULL,
                                  origem VARCHAR(50) NOT NULL,
                                  nome_cliente VARCHAR(255),
                                  valor_total DECIMAL(19, 4) NOT NULL,
                                  status VARCHAR(50) NOT NULL,
                                  data_recebimento TIMESTAMP NOT NULL,
                                  itens JSONB NOT NULL,
                                  payload_original JSONB NOT NULL
);

CREATE INDEX idx_pedidos_externos_origem ON pedidos_externos(origem);
CREATE INDEX idx_pedidos_externos_codigo ON pedidos_externos(codigo_pedido);
CREATE INDEX idx_pedidos_externos_status ON pedidos_externos(status);