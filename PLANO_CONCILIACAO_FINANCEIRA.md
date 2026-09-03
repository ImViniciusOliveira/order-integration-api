# Plano de Conciliação Financeira Shopee e Mercado Livre

## 1. Objetivo

Implementar o fluxo completo de conciliação financeira após um pedido receber o status `COMPLETED`:

```text
n8n
  -> webhook COMPLETED
  -> PostgreSQL
  -> Redis ZSet (120 minutos)
  -> worker
  -> API financeira da plataforma
  -> cálculo/auditoria existente
  -> PostgreSQL
  -> remoção do Redis
  -> webhook de retorno para o n8n
  -> Telegram
```

O n8n deve receber no callback:

- `platform`: `SHOPEE` ou `MERCADOLIVRE`;
- identificador do pedido;
- valores financeiros;
- `has_divergence`;
- detalhamento necessário para escolher a mensagem do Telegram.

O callback posterior não deve depender do payload original do webhook `COMPLETED`, pois ocorre em outra execução do n8n.

## 2. Estado atual

### 2.1 Concluído

- Ingestão de webhooks para Shopee e Mercado Livre.
- Proteção por `X-Internal-API-Key`.
- Validação de canal ativo.
- Event Store imutável em `marketplace_raw_events`.
- Entidade `OrderMaster` em `orders_master`.
- Idempotência básica por plataforma e pedido/status.
- Fila Redis ZSet com membro no formato `PLATFORM:ORDER_SN`.
- Delay inicial configurável de 120 minutos.
- Retry configurável de 30 minutos.
- Worker agendado para processar pedidos prontos.
- Persistência PostgreSQL antes do agendamento no Redis.
- `ShopeeOrderProcessor`.
- `MercadoLivreOrderProcessor`.
- `ShopeeCpfFeeCalculator`.
- `MercadoLivreFeeCalculator`.
- `FeeCalculationResult`.
- `FeeCalculationMapper` para persistência JSONB.
- Serviço de callback HTTP para o n8n.
- Documentos e repositórios MongoDB para credenciais.
- `MarketplaceCredentialService`.
- Testes unitários de calculadoras, processors, fila, worker, ingestão e credenciais.
- Contrato neutro `MarketplaceSettlementClient`.
- Modelo neutro `MarketplaceSettlement`.
- Estados de settlement:
  - `AVAILABLE`;
  - `PENDING`;
  - `RETRYABLE_ERROR`;
  - `PERMANENT_ERROR`.
- Client financeiro da Shopee para `get_escrow_detail`.
- Assinatura Shopee HMAC-SHA256.
- Mapper da resposta Shopee para o modelo neutro.
- Classificação inicial de respostas pendentes, temporárias e definitivas.
- Separação arquitetural por marketplace, sem duplicar calculadores:
  - contratos e modelos compartilhados em `domain/marketplace/common`;
  - processador e calculador próprios em cada marketplace;
  - Shopee separado em `settlement/client`, `settlement/mapper` e `settlement/signer`.
- Integração dos clients financeiros no `EscrowReconciliationServiceImpl`:
  - seleção do client pela plataforma;
  - uso de `shopId` como identificador da conta/vendedor;
  - consulta real via `fetchSettlement(accountId, orderSn)`;
  - remoção do fallback financeiro em `metadata`;
  - tratamento de `AVAILABLE`, `PENDING`, `RETRYABLE_ERROR` e `PERMANENT_ERROR`.
- Identificação de conta Mercado Livre:
  - `MercadoLivreCredentialDocument.sellerId` persistido em `seller_id`;
  - busca por `seller_id`, com fallback técnico para `client_id`;
  - o `seller_id` precisa estar preenchido no documento MongoDB correspondente à loja.
- Auditoria financeira persistida com nomenclatura neutra:
  - `repasse_liquido_real` substitui o nome específico de uma plataforma;
  - o JSONB `metadata.auditoria_financeira` continua sendo produzido pelo mapper compartilhado.
- Validação operacional da stack Docker:
  - PostgreSQL, MongoDB e Redis ativos;
  - Flyway validando cinco migrations;
  - API iniciando sem reinicializações;
  - `/actuator/health` respondendo `HTTP 200` com status `UP`.
- Configuração local do MongoDB corrigida no arquivo ignorado `.env.dev`:
  - `MONGO_HOST=mongodb-order`.

### 2.2 Arquitetura atual de arquivos

Os arquivos de integração devem seguir esta organização. Não criar classes de marketplace dentro de
`domain/order`, nem misturar mapper ou signer dentro de `settlement/client`.

```text
src/main/java/com/dcriar/orderintegration/domain/
├── common/
├── order/
│   ├── entity/
│   ├── repository/
│   └── service/
│       └── impl/
└── marketplace/
    ├── common/
    │   ├── calculator/
    │   │   ├── model/
    │   │   └── mapper/
    │   ├── model/
    │   ├── processor/
    │   └── service/
    ├── shopee/
    │   ├── calculator/
    │   ├── credential/
    │   │   ├── document/
    │   │   ├── repository/
    │   │   └── service/
    │   │       └── impl/
    │   ├── processor/
    │   └── settlement/
    │       ├── client/
    │       ├── mapper/
    │       └── signer/
    └── mercadolivre/
        ├── calculator/
        ├── credential/
        │   ├── document/
        │   ├── repository/
        │   └── service/
        │       └── impl/
        ├── processor/
        └── settlement/
            ├── client/
            ├── mapper/
            └── oauth/
```

Testes devem repetir o mesmo caminho relativo sob:

```text
src/test/java/com/dcriar/orderintegration/domain/marketplace/
```

Arquivos compartilhados devem ficar em `marketplace/common`. Arquivos que conhecem campos ou regras
de uma plataforma devem ficar exclusivamente em `marketplace/shopee` ou `marketplace/mercadolivre`.

### 2.3 Calculadores já existentes

Não criar novos serviços de cálculo. Os calculadores atuais já são responsáveis pela prova real financeira.

#### Shopee CPF

`ShopeeCpfFeeCalculator` já calcula:

- comissão de 14%;
- taxa de transação de 6%;
- taxa fixa de R$ 4,00 por unidade;
- sobretaxa de R$ 5,00 por unidade abaixo de R$ 8,00;
- frete debitado do vendedor;
- repasse teórico;
- diferença entre repasse teórico e real;
- `hasDivergence` com tolerância de R$ 0,05;
- itens auditados e motivo da divergência.

#### Mercado Livre

`MercadoLivreFeeCalculator` já calcula:

- `sale_fee` informado pela API;
- comissão padrão como contingência;
- frete debitado do vendedor;
- repasse teórico;
- diferença entre repasse teórico e real;
- `hasDivergence` com tolerância de R$ 0,05;
- itens auditados e motivo da divergência.

## 3. Lacunas restantes

O fluxo principal já consulta as APIs externas e normaliza a resposta antes do cálculo. Ainda faltam a
validação ponta a ponta com credenciais reais, auditoria operacional completa e políticas de retry/DLQ.

## 4. Modelo interno de settlement

### 4.1 Arquivos já criados

```text
src/main/java/com/dcriar/orderintegration/domain/marketplace/common/model/MarketplaceSettlement.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/common/model/SettlementStatus.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/common/service/MarketplaceSettlementClient.java
```

Shopee:

```text
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/client/ShopeeSettlementClient.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/mapper/ShopeeSettlementResponseMapper.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/signer/ShopeeRequestSigner.java
```

O modelo interno imutável já criado deve ser usado somente para transportar o resultado da API
externa. Não criar outro modelo financeiro paralelo:

- `available`;
- `platform`;
- `orderId`;
- `shopId` ou `sellerId`;
- `grossAmount`;
- `netAmount`;
- `commissionAmount`;
- `transactionFee`;
- `shippingFee`;
- `externalFees`;
- `incomeDetails`;
- `rawFinancialData` sanitizado;
- `pendingReason`;
- `queriedAt`.

Esse modelo não substituirá os calculadores. O uso será:

```java
calculator.calculate(
        order,
        settlement.netAmount(),
        settlement.shippingFee()
);
```

## 5. Próximas etapas de implementação

### Etapa 1 — Fechar contratos internos — CONCLUÍDA

- Contrato, modelo, estados e uso de `BigDecimal` implementados.
- O payload final do callback ainda será fechado durante a integração do reconciler.

### Etapa 2 — Finalizar credenciais MongoDB — PARCIALMENTE CONCLUÍDA

#### Shopee

Validar os campos:

- `shop_id`;
- `partner_id`;
- `live_partner_key`;
- `live_access_token`;
- `live_refresh_token`;
- vencimento do token.

#### Mercado Livre

Validar ou adicionar:

- `client_id`;
- `client_secret`;
- `seller_id` ou `user_id`;
- `live_access_token`;
- `live_refresh_token`;
- `vencimento_token_ts`;
- status da credencial;
- data de atualização.

Regras:

- nunca registrar tokens ou secrets nos logs;
- nunca devolver tokens em respostas REST;
- buscar credencial pela conta correta;
- atualizar tokens renovados no MongoDB;
- confirmar os nomes reais da coleção e dos campos usados pelo n8n.

Implementado:

- Documentos, repositories, services e implementações separados por marketplace.
- Coleção MongoDB atual: `credenciais_lojas`.
- Conexão local validada usando o hostname Docker `mongodb-order`.

Ainda pendente:

- validação completa dos campos reais usados pela conta.

Concluído nesta etapa:

- verificação de `vencimento_token_ts` antes da consulta;
- refresh OAuth2 do Mercado Livre com `grant_type=refresh_token`;
- persistência do access token, refresh token e vencimento no MongoDB;
- preservação do refresh token anterior quando a API não retornar um novo.

### Etapa 3 — Implementar client financeiro da Shopee — PARCIALMENTE CONCLUÍDA

O client, mapper e signer iniciais já foram criados nos caminhos:

```text
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/client/
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/mapper/
src/main/java/com/dcriar/orderintegration/domain/marketplace/shopee/settlement/signer/
```

Implementado:

1. Buscar credencial pelo `shop_id`.
2. Gerar assinatura HMAC-SHA256.
3. Consultar `get_escrow_detail`.
4. Mapear os campos financeiros e `income_details`.
5. Classificar escrow pendente, HTTP 429/5xx e erros definitivos.

Pendente:

1. Verificar validade do access token.
2. Renovar o token se necessário.

Consulta:

```text
GET https://partner.shopeemobile.com/api/v2/payment/get_escrow_detail
```

Parâmetros exigidos:

- `order_sn`;
- `partner_id`;
- `timestamp`;
- `access_token`;
- `shop_id`;
- `sign`.

Mapear:

- `escrow_amount`;
- `buyer_total_amount`;
- `commission_fee`;
- `transaction_fee`;
- `shipping_fee_borne_by_seller`;
- `income_details`.

Retornar settlement pendente quando o escrow ainda não estiver disponível.

### Etapa 4 — Implementar client financeiro do Mercado Livre — PARCIALMENTE CONCLUÍDA

Criar os arquivos somente nestes caminhos:

```text
src/main/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/client/MercadoLivreSettlementClient.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/mapper/MercadoLivreSettlementResponseMapper.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/oauth/MercadoLivreTokenClient.java
```

Testes correspondentes:

```text
src/test/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/client/MercadoLivreSettlementClientTest.java
src/test/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/mapper/MercadoLivreSettlementResponseMapperTest.java
src/test/java/com/dcriar/orderintegration/domain/marketplace/mercadolivre/settlement/oauth/MercadoLivreTokenClientTest.java
```

Implementado nesta etapa:

- client com consulta encadeada de pedido, envio e pagamento;
- autenticação `Authorization: Bearer`;
- mapper para settlement disponível ou pendente;
- classificação inicial de HTTP 429/5xx como erro temporário;
- propriedades tipadas e templates de ambiente atualizados;
- testes unitários do mapper e do contrato do client; a validação HTTP real ficará na etapa de
  integração com o reconciler.

Pendente para concluir a etapa:

- tratamento específico de `money_release_date` conforme o contrato da conta;
- validação com credenciais reais em ambiente controlado.

Fluxo:

1. Buscar credencial da conta/seller no MongoDB.
2. Renovar o access token via OAuth2 quando necessário.
3. Consultar:

```text
GET https://api.mercadolibre.com/orders/{order_id}
```

4. Extrair pagamentos e `shipment_id`.
5. Consultar, quando necessário:

```text
GET https://api.mercadolibre.com/shipments/{shipment_id}
GET https://api.mercadopago.com/v1/payments/{payment_id}
```

6. Consolidar:

- `total_amount`;
- `sale_fee`;
- custos de envio;
- `net_received_amount`;
- `money_release_date`;
- status do pagamento.

7. Considerar que `COMPLETED` não garante liberação financeira imediata.
8. Retornar settlement pendente quando o dinheiro ainda não estiver liberado.

### Etapa 5 — Integrar os clients ao reconciler

Arquivos permitidos para esta etapa:

```text
src/main/java/com/dcriar/orderintegration/domain/order/service/impl/EscrowReconciliationServiceImpl.java
src/main/java/com/dcriar/orderintegration/domain/marketplace/common/service/MarketplaceSettlementClient.java
src/test/java/com/dcriar/orderintegration/domain/order/service/EscrowReconciliationServiceTest.java
```

Substituir a extração simulada de `metadata` por uma consulta externa:

1. Ler o membro do Redis.
2. Localizar o pedido no PostgreSQL.
3. Identificar a plataforma.
4. Selecionar o client correspondente.
5. Buscar as credenciais no MongoDB.
6. Consultar a API externa.
7. Verificar `settlement.available`.
8. Se pendente, reagendar em 30 minutos.
9. Se disponível, executar o calculador já existente.
10. Salvar auditoria e valores no PostgreSQL.
11. Confirmar o save.
12. Remover o pedido do Redis.
13. Enviar callback ao n8n.

A ordem obrigatória permanece:

```text
PostgreSQL -> Redis
```

O pedido só deve ser removido da fila após a persistência bem-sucedida.

### Etapa 6 — Completar auditoria e persistência — PARCIALMENTE CONCLUÍDA

Arquivos previstos:

```text
src/main/java/com/dcriar/orderintegration/domain/marketplace/common/calculator/mapper/FeeCalculationMapper.java
src/main/java/com/dcriar/orderintegration/domain/order/entity/OrderMaster.java
src/main/java/com/dcriar/orderintegration/domain/order/service/impl/EscrowReconciliationServiceImpl.java
src/test/java/com/dcriar/orderintegration/domain/marketplace/common/calculator/mapper/FeeCalculationMapperTest.java
```

Salvar em `metadata.auditoria_financeira`:

- versão da regra;
- data da auditoria;
- `has_divergence`;
- tolerância;
- subtotal;
- quantidade;
- comissão;
- taxa de transação;
- taxas totais;
- frete;
- repasse teórico;
- repasse real;
- diferença;
- motivo;
- itens auditados;
- dados externos sanitizados.

Concluído:

- Revisado o `FeeCalculationMapper` para evitar nomes específicos da Shopee em resultados do Mercado Livre.
- Renomeado `repasse_liquido_shopee` para `repasse_liquido_real`.
- Separados os detalhes específicos em:
  - `ShopeeFeeCalculationDetails`;
  - `MercadoLivreFeeCalculationDetails`.
- Mantido em `FeeCalculationResult` somente o núcleo contábil comum:
  - subtotal;
  - quantidade;
  - taxas totais;
  - frete;
  - repasse teórico e real;
  - diferença;
  - divergência.
- O mapper persiste os detalhes específicos em `detalhes_plataforma`, sem aumentar o contrato comum
  quando uma nova plataforma for adicionada.

Ainda pendente:

- incluir no JSONB os dados externos sanitizados do settlement;
- revisar a inclusão dos dados externos no contrato de auditoria.

Exemplo do nome anterior:

```text
repasse_liquido_shopee
```

Nome neutro utilizado:

```text
repasse_liquido_real
comissao
taxa_transacao
```

### Etapa 7 — Implementar retry, limite e DLQ

Novos componentes, se necessários, devem ser criados em:

```text
src/main/java/com/dcriar/orderintegration/domain/order/service/
src/main/java/com/dcriar/orderintegration/domain/order/service/impl/
src/main/java/com/dcriar/orderintegration/config/
```

Não criar retry ou DLQ dentro de clients de marketplace; clients devem consultar e classificar a
resposta, enquanto o serviço de reconciliação deve orquestrar reagendamento e limite de tentativas.

- Settlement pendente: reagendar em `ESCROW_RETRY_DELAY_MINUTES`.
- HTTP 429: reagendar.
- HTTP 5xx: reagendar.
- Timeout: reagendar.
- Token expirado: renovar e repetir.
- Credencial ausente: erro explícito.
- Pedido inválido ou HTTP 4xx definitivo: não repetir indefinidamente.
- Contabilizar tentativas por pedido.
- Respeitar `ESCROW_MAX_RETRIES`.
- Mover pedidos excedentes para DLQ Redis.
- Evitar processamento concorrente duplicado do mesmo pedido.

Implementado nesta etapa:

- contador Redis individual por pedido;
- limpeza do contador após sucesso ou encerramento;
- limite configurado por `ESCROW_MAX_RETRIES`;
- DLQ derivada da chave principal (`<fila>:dlq`);
- remoção da fila principal quando o erro é definitivo ou excede o limite.

### Etapa 8 — Finalizar callback para o n8n

O serviço deve enviar:

- `platform`;
- `order_sn` ou `order_id`;
- `shop_id` ou `seller_id`;
- `has_divergence`;
- subtotal;
- repasse real;
- repasse teórico;
- comissão;
- taxa de transação;
- frete;
- total de taxas;
- diferença;
- motivo da divergência;
- versão da regra;
- detalhes financeiros.

Implementado parcialmente:

- callback envia `platform`, `order_sn`, `shop_id`, valores principais, indicador de divergência e
  todo o bloco `auditoria_financeira`;
- falhas HTTP do `RestClient` são registradas sem interromper a persistência financeira.

Ainda pendente:

- outbox ou retry próprio para garantir reentrega do callback;
- teste de integração com o webhook n8n.

O n8n utilizará:

```text
platform
has_divergence
```

para selecionar a mensagem e decidir se o Telegram exibirá divergência ou conciliação normal.

### Etapa 9 — Centralizar configurações — PARCIALMENTE CONCLUÍDA

Implementado:

- `RestClient.Builder` global em:

```text
src/main/java/com/dcriar/orderintegration/config/RestClientConfig.java
```

- Shopee e Mercado Livre recebem o builder por injeção de construtor.
- Cada client cria sua própria instância `RestClient`, mantendo URLs e autenticações isoladas.
- Teste da configuração em:

```text
src/test/java/com/dcriar/orderintegration/config/RestClientConfigTest.java
```

Adicionar em `OrderIntegrationProperties`:

- URLs base;
- paths;
- timeouts;
- endpoints OAuth2;
- delay e retry;
- limite de tentativas;
- chave da DLQ;
- políticas de consulta.

Atualizar:

- `application.yaml`;
- `application-dev.yaml`;
- `.env.dev.example`;
- `.env.prod.example`;
- `docker-compose.dev.yml`.

Não adicionar fallbacks silenciosos em configurações de negócio.

### Etapa 10 — Testar ponta a ponta

Arquivos de teste novos devem permanecer no mesmo pacote relativo da classe testada sob
`src/test/java`. Não criar testes em um pacote genérico `domain/order` para classes específicas de
Shopee ou Mercado Livre.

Testar:

- credencial encontrada;
- credencial ausente;
- access token válido;
- access token expirado;
- refresh Shopee;
- refresh Mercado Livre;
- assinatura Shopee;
- resposta Shopee disponível;
- resposta Shopee pendente;
- resposta Mercado Livre com pagamento liberado;
- resposta Mercado Livre ainda pendente;
- shipment ausente;
- timeout;
- HTTP 429;
- HTTP 5xx;
- erro definitivo;
- cálculo sem divergência;
- cálculo com divergência;
- retry de 30 minutos;
- limite de tentativas;
- DLQ;
- persistência antes da remoção do Redis;
- callback n8n;
- seleção correta da mensagem do Telegram.

## 6. Critérios de conclusão

Considerar a implementação concluída somente quando:

- Shopee e Mercado Livre forem consultados por clients reais.
- Tokens forem obtidos e renovados pelo MongoDB.
- Respostas externas forem mapeadas sem depender de valores financeiros simulados no `metadata`.
- Os calculadores existentes forem reutilizados.
- O resultado financeiro for persistido com auditoria completa.
- `has_divergence` for calculado pelo backend.
- O pedido só sair do Redis após salvar no PostgreSQL.
- Retries e DLQ estiverem protegidos contra loops infinitos.
- O callback chegar ao n8n com `platform` e `has_divergence`.
- Testes existentes e novos testes do fluxo passarem.

## 7. Regras arquiteturais obrigatórias

- PostgreSQL é a fonte primária de verdade.
- Redis é apenas o mecanismo de agendamento.
- Webhooks brutos devem ser preservados no Event Store.
- Credenciais não podem aparecer em logs ou respostas.
- Configurações devem usar `@ConfigurationProperties`.
- Valores financeiros devem usar `BigDecimal`.
- Alterações de schema devem usar Flyway.
- Services devem ter contrato e implementação separados.
- DTOs devem permanecer puros.
- Controllers não acessam repositories diretamente.
- Não usar `@Value`, `@Autowired`, `@Data`, `BeanUtils.copyProperties` ou `System.out`.
- Não fazer `trim()` manual em services/controllers.
- Preservar JavaDocs existentes.
- Manter alterações cirúrgicas e cobertas por testes.
