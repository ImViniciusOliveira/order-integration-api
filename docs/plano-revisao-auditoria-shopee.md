# Plano futuro de revisao da auditoria financeira Shopee

## Decisao atual

Manter a regra de auditoria como esta e observar novos pedidos antes de alterar o
calculador. A divergencia analisada sera tratada como caso de estudo, sem
correcao retroativa neste momento.

## Evidencias persistidas no PostgreSQL

Para o pedido analisado, o banco preservou:

- os eventos brutos `COMPLETED`;
- o evento bruto `SETTLEMENT_ESCROW_RESPONSE`;
- o settlement completo em `orders_master.metadata.settlement_financial_details`;
- o resultado da prova real em `orders_master.metadata.auditoria_financeira`;
- o status `RECONCILED_WITH_DIVERGENCE`;
- o repasse real em `escrow_amount`;
- o valor de frete usado na auditoria em `shipping_fee_borne_by_seller`.

A tabela `marketplace_raw_events` permanece como fonte imutavel do payload
recebido. O campo `metadata` de `orders_master` preserva o snapshot financeiro
e o resultado calculado para comparacao posterior.

## Caso observado

No caso estudado, a Shopee retornou:

- subtotal dos itens: R$ 11,50;
- comissao: R$ 2,07;
- taxa de servico: R$ 4,63;
- `final_shipping_fee`: -R$ 3,87;
- `escrow_amount`: R$ 4,80.

A regra atual calculou R$ 8,67 porque aplicou o frete negativo como uma
subtracao adicional. O valor real de R$ 4,80 coincide com o subtotal menos as
taxas de R$ 6,70, mas essa interpretacao ainda deve ser validada com outros
pedidos e com a documentacao oficial da Shopee.

## Revisao futura

Antes de alterar a regra, coletar e comparar uma amostra de settlements com:

1. frete efetivamente cobrado do vendedor;
2. frete subsidiado pela Shopee;
3. frete pago pelo comprador;
4. frete gratis;
5. reembolsos, devolucoes e ajustes;
6. valores positivos e negativos em `final_shipping_fee`;
7. pedidos sem divergencia e pedidos com divergencia confirmada.

Para cada pedido, comparar:

```text
repasse_base =
    subtotal
    - comissao
    - taxa de servico
    - taxa de transacao
    - outras taxas
```

Depois confrontar o resultado com `escrow_amount` e identificar se o residuo e
explicado por `final_shipping_fee` ou por outro ajuste oficial. A regra futura
nao deve ignorar sinais indiscriminadamente nem aplicar o mesmo ajuste duas
vezes.

## Comando de acompanhamento

Para listar novas divergencias persistidas:

```bash
./scripts/analyze-financial-divergence.sh
```

Para analisar todos os casos desde uma data:

```bash
./scripts/analyze-financial-divergence.sh 2026-09-07
```

Somente depois de reunir essa amostra e confirmar a semantica dos campos da
Shopee, revisar o `ShopeeCpfFeeCalculator` e criar testes para cada cenario.
