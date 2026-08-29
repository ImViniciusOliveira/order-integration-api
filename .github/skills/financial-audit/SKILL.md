---
name: financial-audit
description: Regras e padrões de arquitetura para auditoria financeira, conciliação de Escrow, prova real matemática de taxas e ingestão segura/idempotente de webhooks em e-commerce.
---

# Auditoria Financeira, Ingestão Segura & Conciliação de Escrow

## 1. Ingestão Segura de Webhooks (Internal API Key)
- Endpoints de ingestão de webhooks recebem payloads sensíveis de gateways (ex: n8n) e de marketplaces.
- Toda rota de webhook (`POST /api/v1/webhooks/{platform}`) deve exigir autenticação via cabeçalho HTTP estático `X-Internal-API-Key`.
- A chave é configurada fortemente tipada no `application.yml` (`application.security.internal-api-key`) e validada no Controller antes do processamento.

## 2. Idempotência e Prevenção contra *Retry Storm*
- Marketplaces e plataformas de automação frequentemente realizam retentativas de envio de webhooks em caso de lentidão transitória na rede.
- Antes de processar uma ingestão, o serviço deve verificar se o pedido já existe no banco com o mesmo status recebido.
- Se o status atual do banco já for igual ao recebido, o sistema deve ignorar o processamento e responder `HTTP 200 OK` silenciosamente, evitando reprocessamentos redundantes e inconsistências de fila.

## 3. Event Store Imutável (Auditoria Bruta de Payloads)
- Todo webhook recebido deve ser salvo integralmente no banco relacional (`marketplace_raw_events`) com seu payload JSON bruto **antes** de qualquer execução de regras de negócio ou mutação de estado.
- Garante comprovação jurídica, reprodutibilidade de bugs e suporte a contestações financeiras junto ao marketplace.

## 4. Ordem Obrigatória de Processamento de Escrow (PostgreSQL Primeiro, Redis Depois)
- Ao processar um pedido finalizado (`COMPLETED`):
  1. O estado do pedido é atualizado e comitado primeiro no PostgreSQL (`orders_master`).
  2. Somente após a confirmação transacional do banco, o identificador do pedido (`PLATFORM:ORDER_SN`) é agendado no Sorted Set do Redis com delay em minutos (ex: 120 min).
  3. O PostgreSQL é a fonte primária de verdade e o Redis é apenas o motor de agendamento temporal.

## 5. Prova Real Matemática de Taxas (Matemática Fixa CPF)
- O motor de conciliação financeira realiza a **prova real** comparando a fórmula matemática oficial parametrizada para contas Pessoa Física (CPF) com os valores detalhados no nó `income_details` retornado pela API de Escrow da plataforma:
  - **Sem divergência:** O pedido é atualizado com o valor líquido oficial e marcado como `reconciled = true`.
  - **Com divergência:** O pedido é sinalizado com `has_divergence = true` e o detalhamento das taxas ocultas/indevidas é persistido na coluna `metadata` JSONB para auditoria e disputa.

## 6. Auditoria de Motivos de Cancelamento
- Ao receber status de cancelamento (`CANCELLED`), o processador deve extrair o campo `cancel_reason` e persisti-lo no `metadata` JSONB.
- Permite segregar relatórios financeiros entre falhas de pagamento/risco antifraude e cancelamentos solicitados pelo comprador.
