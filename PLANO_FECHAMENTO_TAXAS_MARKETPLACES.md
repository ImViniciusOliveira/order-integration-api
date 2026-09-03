# Pendencias de Taxas e Auditoria Financeira

Este documento contem somente atividades ainda nao concluidas para fechar a
auditoria financeira da Shopee e do Mercado Livre.

## 1. Impedir calculos sem dados oficiais

- Nao marcar o pedido como conciliado quando faltarem valores oficiais.
- Nao usar porcentagens padrao silenciosas.
- Classificar o caso como `AUDIT_INCOMPLETE`.
- Registrar os campos financeiros ausentes.
- Reagendar a consulta quando o settlement ainda nao estiver fechado.
- Notificar: `Dados insuficientes para concluir a auditoria financeira`.

## 2. Corrigir a taxa fixa do Mercado Livre

- Identificar os itens elegiveis conforme a regra comercial vigente.
- Calcular a taxa por unidade e respeitar a quantidade.
- Diferenciar taxa oficial da API e valor estimado.
- Persistir o valor e a origem no detalhamento financeiro.
- Cobrir os limites de R$ 79,00, itens abaixo do limite, varias unidades e itens
  mistos.

## 3. Remover fallback silencioso de comissao do Mercado Livre

- Usar somente `sale_fee` oficial quando estiver presente.
- Quando `sale_fee` estiver ausente, nao aplicar automaticamente 11,5%.
- Classificar a auditoria como `AUDIT_INCOMPLETE`.
- Registrar o motivo e os campos ausentes.
- Reagendar quando a ausencia indicar settlement ainda incompleto.

## 4. Confirmar regras comerciais vigentes

Validar e versionar, com data de vigencia e origem documental:

- tipo de vendedor;
- categoria do produto;
- tipo de anuncio;
- frete ou fulfillment;
- campanhas e descontos;
- regras de baixo valor;
- taxas adicionais;
- base usada pela plataforma no calculo.

## 5. Persistir snapshot financeiro externo

Preservar a resposta normalizada usada na auditoria para reprocessamento e
contestacao, sem salvar tokens ou credenciais. O snapshot deve conter:

- plataforma e pedido;
- instante e status da consulta;
- valor bruto, repasse, taxas e frete;
- `income_details` ou estrutura equivalente;
- resposta externa normalizada;
- identificador da requisicao externa, quando fornecido.

## 6. Diferenciar estados de auditoria

Formalizar e persistir os estados:

- `RECONCILED`;
- `RECONCILED_WITH_DIVERGENCE`;
- `AUDIT_INCOMPLETE`;
- `PENDING_SETTLEMENT`;
- `PERMANENT_ERROR`.

Ausencia de dados nao deve ser tratada como divergencia financeira.

## 7. Confiabilidade das notificacoes n8n

Implementar outbox no PostgreSQL para que uma falha do n8n, ngrok ou Telegram
nao perca a notificacao:

- persistir o evento antes da entrega;
- controlar `PENDING`, `DELIVERED` e `FAILED`;
- aplicar retry com backoff e limite;
- registrar todas as tentativas e o ultimo erro;
- usar idempotency key por pedido e evento;
- permitir reprocessamento sem apagar o historico.

## 8. Historico permanente da DLQ

Registrar no PostgreSQL:

- pedido, plataforma e etapa que falhou;
- data e hora de cada tentativa;
- quantidade de tentativas;
- resposta externa sanitizada;
- status do pedido;
- dados financeiros recebidos;
- motivo da falha;
- status e acao de reprocessamento.

A DLQ do Redis deve permanecer somente como mecanismo operacional de
reprocessamento.

## 9. Proteger concorrencia da fila Redis

Implementar claim atomico ou lock por `PLATFORM:ORDER_SN` para impedir que duas
instancias processem o mesmo pedido simultaneamente.

## 10. Validacao pendente do Mercado Livre

Executar o fluxo completo com pedido real:

- pagamento liberado;
- `sale_fee` por item;
- frete e shipment;
- taxa fixa;
- divergencia e auditoria incompleta;
- renovacao de token;
- callback normal e com divergencia.

## 11. Validacao final

Considerar o escopo fechado somente após:

- regras comerciais confirmadas e versionadas;
- taxa fixa do Mercado Livre implementada;
- snapshot externo persistido;
- auditoria incompleta diferenciada;
- outbox e DLQ permanente implementados;
- concorrencia Redis protegida;
- pedido Mercado Livre real validado ponta a ponta.
