---
name: project-context
description: Use esta skill no início de qualquer nova tarefa ou quando precisar entender as regras de negócio, a infraestrutura ou o estado atual do projeto.
---

# Contexto do Projeto

## 1. Leitura obrigatória antes de qualquer tarefa

Antes de sugerir arquiteturas complexas, criar módulos inteiros ou iniciar qualquer nova tarefa, leia obrigatoriamente:

1. esta skill (`.github/skills/project-context/SKILL.md`)
2. o arquivo `docs/arquitetura.md`

Esses dois arquivos são a referência principal do projeto. Eles definem o roadmap, a infraestrutura (Docker) e as decisões de negócio exclusivas deste sistema.

## 2. Visão do Hub Multiplataforma (dcriar-order-integration-api)
- Microsserviço central do ecossistema DCriar para integração de pedidos de múltiplos marketplaces (Shopee, TikTok Shop, Mercado Livre, Amazon).
- **Persistência Híbrida (Relacional + JSONB):** Colunas relacionais para core financeiro e filtros (`platform`, `shop_id`, `order_sn`, `status`, `reconciled`, `escrow_amount`) + coluna curinga `metadata JSONB` com índice GIN para absorver dados heterogêneos.
- **Precisão Financeira `DECIMAL(15,4)`:** Valores monetários de frete, repasse e taxas usam `DECIMAL(15,4)` no PostgreSQL e `BigDecimal` no Java.
- **Canais Dinâmicos no Banco:** Plataformas são cadastradas na tabela `marketplace_channels` (não usar Enums fixos).
- **Event Store Imutável:** Webhooks gravados imediatamente em `marketplace_raw_events` antes do processamento.
- **Ciclo de Vida em 2 Fases:** 
  * Fase 1: Despacho com frete estimado (`estimated_shipping_fee`).
  * Fase 2: Pós-entrega com conciliação real de Escrow via fila com delay no Redis.
