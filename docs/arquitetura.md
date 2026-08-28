# Arquitetura e Decisões de Projeto (`arquitetura.md`)

Este documento descreve a visão do produto, diretrizes arquiteturais, regras de infraestrutura e decisões de negócio adotadas neste projeto. **Qualquer IA ou desenvolvedor atuando neste repositório deve ler e seguir estas regras antes de propor novas funcionalidades.**

---

## 1. Visão Geral do Projeto (dcriar-order-integration-api)

O **dcriar-order-integration-api** é o microsserviço central do ecossistema **DCriar** projetado como um **Hub Multiplataforma de Ingestão, Ciclo de Vida e Conciliação Financeira de Pedidos de E-commerce** (integrando inicialmente **Shopee** e **TikTok Shop**, com arquitetura expansível para Mercado Livre, Amazon e novos canais).

### O que esta API faz na prática:
1. **Ingestão Resiliente de Webhooks:** Recebe notificações de eventos brutos (status de pedidos, código de rastreio, cancelamentos) capturadas pelo `n8n` ou enviadas diretamente pelas plataformas, persistindo imediatamente no *Event Store* para auditoria imutável antes de qualquer processamento.
2. **Ciclo de Vida em Duas Fases (Estimativa vs. Conciliação Real):**
   * **Fase 1 (Despacho / READY_TO_SHIP):** Grava o pedido com o provisionamento inicial de status e a **taxa de frete estimada** (`estimated_shipping_fee`).
   * **Fase 2 (Pós-Entrega / COMPLETED / Escrow Settlement):** Gerencia a fila de conciliação com delay via **Redis** para buscar o extrato definitivo na API de *Escrow/Settlement*, atualizando o valor líquido real repassado (`escrow_amount`), o custo de frete real cobrado do vendedor (`shipping_fee_borne_by_seller`) e marcando o pedido como conciliado (`reconciled = true`).
3. **Mapeamento Agnóstico & Anti-Refatoração:** Converte estruturas de dados heterogêneas dos múltiplos marketplaces para o modelo unificado de domínio através de MapStruct e armazenamento híbrido com JSONB.
4. **Filtros e Relatórios Operacionais:** Fornece consultas dinâmicas de alta performance para monitoramento de pedidos por canal, loja, status de conciliação e períodos.

---

## 1.1. Arquitetura de Componentes
O ecossistema roda de forma conteinerizada via Docker Compose, dividido nos seguintes serviços interconectados:
- **API Spring Boot (Java 26 / Spring Boot 4):** Expõe os endpoints REST, orquestra as estratégias de cada canal (Shopee, TikTok, etc.), aplica as regras de negócio de domínio rico e gerencia a persistência via Hibernate/JPA.
- **Banco de Dados (PostgreSQL):** Versionamento automatizado com **Flyway**, utilizando tabelas relacionais para dados core e campos JSONB indexados com GIN para dados dinâmicos de pedidos.
- **Motor de Automação (`n8n`):** Captura webhooks externos, trata autenticações/assinaturas e repassa eventos padronizados para a API.
- **Fila e Cache (`Redis`):** Fila com delay para agendamento da conciliação pós-venda (*Escrow*) e controle de idempotência.

---

## 2. Topologia e Infraestrutura (Docker e Compose)
O projeto adota uma estrutura flexível de containers para desenvolvimento e produção, baseada na combinação de arquivos Compose:

* **Modo de Desenvolvimento (Backend na IDE):** Utiliza-se apenas o arquivo `docker-compose.dev.yml` para subir **exclusivamente** as dependências de infraestrutura (PostgreSQL, MinIO, Redis, etc.). Neste cenário, a aplicação Spring Boot é rodada diretamente pela IDE, permitindo debug ágil e *hot-reload*.
* **Modo de Desenvolvimento Full (Docker + Override):** Ao executar o ambiente utilizando o `docker-compose.dev.yml` em conjunto com o `docker-compose.override.yml`, a stack completa é levantada, **incluindo o próprio backend rodando via container**. Isso serve para validar localmente o comportamento real da aplicação "containerizada" antes do deploy.
* **Ambiente de Produção:** O arquivo `docker-compose.prod.yml` é estritamente isolado e contém a configuração otimizada e segura de fato para o deploy em produção.
* **Atualização Obrigatória:** Qualquer nova dependência externa adicionada ao sistema deve ter seu container correspondente mapeado e sincronizado nos arquivos de dev e prod.

---

## 3. Padrões Universais de Engenharia (Código e Persistência)
*Diretrizes globais de qualidade que devem ser seguidas em todo o código Java/Spring deste repositório:*

* **Documentação Obrigatória (JavaDoc):** Todas as classes, interfaces e records de negócio e domínio devem conter JavaDoc no topo (`/** ... */`) explicando sua responsabilidade arquitetural. Métodos de domínio rico e com regras complexas devem conter documentação técnica de seus parâmetros (`@param`) e regras.
* **Modelo de Domínio Rico (Rich Domain Model):** Proibido criar entidades anêmicas. Mudanças de estado e validações de negócio devem ocorrer através de métodos expressivos dentro da própria entidade (ex: `conciliarEscrow(...)`, `provisionarEstimativa(...)`, `atualizarRastreio(...)`, `ativar()`, `desativar()`).
* **Lombok sem Anemia:** Proibido `@Data` e `@Value`. Uso estrito de `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor` e `@AllArgsConstructor`.
* **Injeção de Dependências:** Proibido `@Autowired`. Injeção exclusivamente por construtor utilizando `private final` e anotação `@RequiredArgsConstructor` do Lombok.
* **Auditoria Centralizada:** Todas as novas tabelas de negócio devem obrigatoriamente estender a classe `AuditableEntity` para garantir rastreabilidade automática com `OffsetDateTime`.
* **Fuso Horário (Timezone):** Banco com `TIMESTAMPTZ`, Java com `OffsetDateTime` e `ApplicationTimeZoneConfig` travado em `America/Sao_Paulo`.
* **Consultas e Filtros Dinâmicos (Specifications):** É proibido o uso de *query methods* longos ou `@Query` manuais nos Repositories. Toda busca com múltiplos filtros dinâmicos DEVE ser implementada utilizando **Spring Data JPA Specifications** (na pasta `domain/specification`).
* **Busca Textual Avançada:** Ao implementar *Specifications* para buscas de texto, deve-se obrigatoriamente utilizar a classe utilitária `PostgresSearchUtils` do projeto para lidar com normalizações e acentuação no banco via `unaccent`.
* **Mapeamento de Objetos:** Uso estrito de **MapStruct**. Proibido `BeanUtils.copyProperties`.

---

## 4. Normalização de Entrada de Dados (Trim Automático)
* **O Problema:** Usuários e integrações costumam enviar espaços em branco acidentais no início ou no fim de textos em formulários e JSONs (ex: `"  Produto X   "`). Tratar isso manualmente em cada regra de negócio suja o código.
* **A Solução (Jackson Global):** A aplicação possui o `TrimStringDeserializer` registrado globalmente no Jackson 3.
* **Regra para a IA:** A IA **não deve** criar códigos manuais de limpeza de texto (como `string.trim()`) nos Services ou Controllers. O framework de serialização já faz isso de forma transparente na entrada.

---

## 5. Armazenamento de Arquivos (Storage)
* **Abstração S3/MinIO:** Todo e qualquer armazenamento de arquivo (imagens, PDFs, etiquetas, relatórios) deve utilizar o serviço de Storage compatível com S3 (ex: MinIO) configurado na infraestrutura.

---

## 6. Manutenção Viva da Documentação e Configurações
* **Variáveis de Ambiente:** Nenhuma senha ou credencial real deve ser commitada no repositório. Sempre que uma nova chave for adicionada aos arquivos de configuração, os respectivos templates de exemplo (`.env.dev.example` e `.env.prod.example`) DEVEM ser atualizados imediatamente.
* **Evolução do README:** O `README.md` principal é um documento vivo. Se a arquitetura mudar, novos comandos de build forem criados ou novas ferramentas forem adotadas, o README deve refletir essas mudanças no mesmo commit.

---

## 7. Configurações Locais e Variáveis de Ambiente na IDE
* A aplicação suporta perfis locais (`application-local.yml`) e o carregamento de variáveis via arquivos `.env` locais que são explicitamente ignorados pelo Git (`.gitignore`).
* Apenas arquivos de exemplo (`.env.dev.example` e `.env.prod.example`) sobem para o repositório.

---

## 8. Ideias e Decisões Específicas deste Projeto (Hub Multiplataforma)
*As decisões abaixo foram adotadas especificamente para atender aos desafios de integração com múltiplos Marketplaces (Shopee, TikTok Shop, Mercado Livre, Amazon):*

* **Persistência Híbrida (Relacional + JSONB):** O banco utiliza colunas relacionais fixas para campos financeiros e filtros críticos (`platform`, `shop_id`, `order_sn`, `status`, `reconciled`, `escrow_amount`) combinadas com a coluna curinga `metadata JSONB` (com índice `GIN`). Isso absorve itens, SKUs, variações e metadados de qualquer marketplace sem necessidade de `ALTER TABLE`.
* **Precisão Monetária Máxima `DECIMAL(15,4)`:** Todos os valores financeiros de estimativa, frete cobrado e repasse líquido utilizam `DECIMAL(15,4)` no PostgreSQL e `BigDecimal` no Java para eliminar perdas em microcentavos de comissões e taxas fracionadas de e-commerce.
* **Canais Dinâmicos no Banco (`marketplace_channels`):** Plataformas de venda não são travadas em Enums rígidos no Java. São cadastradas e gerenciadas dinamicamente na tabela `marketplace_channels` (com seed inicial para `SHOPEE`, `TIKTOK`, `MERCADO_LIVRE`, `AMAZON`), permitindo que novos canais sejam ativados sem refatoração do código base.
* **Event Store Imutável (`marketplace_raw_events`):** Todo webhook recebido de qualquer marketplace é gravado imediatamente em formato append-only com o payload JSON bruto completo antes de ser processado pelo sistema, garantindo auditoria e reprodutibilidade de eventos.
