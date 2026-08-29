---
name: project-context
description: Use esta skill no início de qualquer nova tarefa ou quando precisar entender as regras de negócio, a infraestrutura ou o estado atual do projeto.
---

# Contexto do Projeto e Decisões de Domínio

## 1. Leitura Obrigatória
Antes de propor funcionalidades ou criar novos módulos, leia obrigatoriamente:
1. esta skill (`.github/skills/project-context/SKILL.md`)
2. a skill de auditoria (`.github/skills/financial-audit/SKILL.md`)
3. o arquivo `docs/arquitetura.md`

## 2. Visão do Hub de Integração de Pedidos
- Centralizador, auditor financeiro e orquestrador de pedidos de marketplaces (Shopee e novos canais).
- **Persistência Híbrida (Relacional + JSONB):** Colunas relacionais para core financeiro e filtros (`platform`, `shop_id`, `order_sn`, `status`, `reconciled`, `escrow_amount`) + coluna curinga `metadata JSONB` com índice GIN para absorver dados heterogêneos.
- **Precisão Financeira `DECIMAL(15,4)`:** Valores monetários de frete, repasse e taxas usam `DECIMAL(15,4)` no PostgreSQL e `BigDecimal` no Java.
- **Event Store Imutável:** Webhooks gravados imediatamente em `marketplace_raw_events` antes de qualquer processamento.
- **Segurança de Ingestão:** Webhooks protegidos pelo cabeçalho `X-Internal-API-Key`.
- **Idempotência Automática:** Evita reprocessamento redundante em reenvios de webhooks com mesmo status.
- **Ciclo de Vida em 2 Fases:**
  * Fase 1: Despacho com frete estimado (`estimated_shipping_fee`).
  * Fase 2: Pós-entrega com conciliação real de Escrow via fila com delay no Redis e prova real de taxas CPF.

## 3. Ordem de Execução Obrigatória (PostgreSQL Primeiro, Redis Depois)
Ao processar um pedido com status `COMPLETED`:
1. **Passo 1 (PostgreSQL):** Salvar e confirmar primeiro o estado do pedido no banco relacional (`orders_master`).
2. **Passo 2 (Redis):** Somente após o sucesso no banco, enfileirar o pedido no Sorted Set do Redis com score de timestamp Unix correspondente ao delay configurado (ex: 120 minutos).

## 4. Fila de Conciliação Redis (Delay Queue ZSet)
- Chave e delay são configuráveis via variáveis de ambiente (`ESCROW_REDIS_QUEUE_KEY`, `ESCROW_DELAY_MINUTES`).
- Mapeados via `@ConfigurationProperties` em `OrderIntegrationProperties`.
- O score armazena o timestamp de liberação em milissegundos.
- O membro do ZSet segue o formato `"PLATFORM:ORDER_SN"`.
