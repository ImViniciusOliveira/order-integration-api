# Arquitetura e Decisões de Projeto (`arquitetura.md`)

Este documento descreve a visão do produto, diretrizes arquiteturais, regras de infraestrutura e decisões de negócio adotadas neste projeto. **Qualquer IA ou desenvolvedor atuando neste repositório deve ler e seguir estas regras antes de propor novas funcionalidades.**

---

## 1. Visão Geral do Projeto (dcriar-order-integration-api)

O **dcriar-order-integration-api** é o microsserviço central do ecossistema **DCriar** projetado como um **Hub Multiplataforma de Ingestão, Auditoria e Conciliação Financeira de Pedidos de E-commerce** (integrando inicialmente **Shopee**, com arquitetura expansível para TikTok Shop, Mercado Livre, Amazon e novos canais).

### O que esta API faz na prática:
1. **Ingestão Resiliente e Segura de Webhooks:** Recebe notificações de eventos brutos capturadas pelo `n8n` ou enviadas diretamente pelas plataformas sob proteção de chave interna (`X-Internal-API-Key`), persistindo imediatamente no *Event Store* para auditoria imutável antes de qualquer processamento.
2. **Idempotência Automática (Anti-Retry Storm):** Valida previamente o estado do pedido no banco de dados. Caso um webhook reenviado possua o mesmo status já registrado, responde `HTTP 200 OK` silenciosamente sem duplicar processamentos nem poluir a fila.
3. **Ciclo de Vida em Duas Fases (Estimativa vs. Conciliação Real):**
   * **Fase 1 (Despacho / READY_TO_SHIP):** Grava o pedido com o provisionamento inicial de status e a **taxa de frete estimada** (`estimated_shipping_fee`).
   * **Fase 2 (Pós-Entrega / COMPLETED / Escrow Settlement):** Gerencia a fila de conciliação com delay via **Redis (ZSet)** para buscar o extrato definitivo na API de *Escrow/Settlement*, atualizando o valor líquido real repassado (`escrow_amount`) e o custo de frete real cobrado do vendedor (`shipping_fee_borne_by_seller`).
4. **Motor de Prova Real de Taxas & Auditoria Financeira:**
   * Executa a fórmula matemática oficial para contas Pessoa Física (CPF), confrontando o cálculo teórico com os nós detalhados de `income_details` da plataforma.
   * Identifica cobranças indevidas, fretes duplicados ou taxas fantasmas, marcando o pedido com `has_divergence = true` e registrando a auditoria no `metadata` JSONB.
5. **Auditoria de Cancelamentos e Motivos:**
   * Ao receber cancelamentos (`CANCELLED`), isola o motivo (`cancel_reason`) no JSONB, permitindo diferenciar problemas de risco/antifraude no cartão de desistências do comprador.
6. **Mapeamento Agnóstico & Anti-Refatoração:** Converte estruturas de dados heterogêneas dos múltiplos marketplaces para o modelo unificado de domínio através de MapStruct e armazenamento híbrido com JSONB.
7. **Filtros, Consultas e Paginação de Alta Performance (HATEOAS):** Fornece endpoints com suporte nativo a paginação (`Pageable`, `PagedModel`/HATEOAS) e filtros dinâmicos via Specifications para monitoramento de pedidos por canal, loja, status de conciliação, divergências financeiras e períodos.

---

## 2. Topologia e Infraestrutura (Docker e Compose)
O ecossistema roda de forma conteinerizada via Docker Compose, dividido nos seguintes serviços interconectados:
- **API Spring Boot (Java / Spring Boot):** Expõe os endpoints REST, orquestra as regras de negócio de domínio rico e gerencia a persistência via Hibernate/JPA.
- **Banco de Dados (PostgreSQL):** Versionamento automatizado com **Flyway**, utilizando tabelas relacionais para dados core e campos JSONB indexados com GIN para dados dinâmicos.
- **Motor de Automação / Filas:** Serviços auxiliares de mensageria e orquestração (Redis) para controle assíncrono e agendamento da conciliação de Escrow.

---

## 3. Padrões Universais de Engenharia (Código e Persistência)
*Diretrizes globais de qualidade que devem ser seguidas em todo o código Java/Spring:*

* **Documentação Obrigatória (JavaDoc) e Preservação Estrita:** Todas as classes, interfaces e records de negócio e domínio devem conter JavaDoc no topo (`/** ... */`) explicando sua responsabilidade arquitetural em português. Métodos de domínio rico e com regras complexas devem conter documentação técnica de seus parâmetros (`@param`), retornos (`@return`) e exceções (`@throws`).
  * **Regra de Ouro da Preservação:** Ao enriquecer uma classe existente (ex: adicionar anotações OpenAPI/Swagger `@Schema`), é **estritamente proibido** apagar, truncar ou reescrever JavaDocs pré-existentes. Apenas acrescente anotações ou atualize cirurgicamente os `@param` caso a assinatura do método mude.
* **Princípio da Menor Alteração (Edições Cirúrgicas):** É proibido reescrever arquivos inteiros ou dezenas de linhas de testes/código quando a demanda exigir apenas pequenas alterações ou adições pontuais. Modifique apenas o estritamente necessário para manter o histórico do Git limpo, focado e previsível.
* **Modelo de Domínio Rico (Rich Domain Model):** Proibido criar entidades anêmicas. Mudanças de estado e validações de negócio devem ocorrer através de métodos expressivos dentro da própria entidade.
* **Isolamento de Camadas (Interface vs. Implementação):** Todo serviço de negócio DEVE declarar sua **interface de contrato** no pacote `domain.[subdominio].service` e sua **classe de implementação** concreta no pacote `domain.[subdominio].service.impl`.
* **Gerenciamento Transacional (`@Transactional`):** 
  * As classes de implementação de serviço devem ser anotadas no topo com `@Transactional(readOnly = true)`.
  * Métodos de escrita e alteração de estado devem ser anotados explicitamente com `@Transactional`.
* **DTO Records 100% Puros e Anêmicos:** Records nos pacotes `api.dto` devem ser exclusivamente transportadores imutáveis de dados, sem métodos internos de conversão (`toEntity()`, `toCriteria()`). Todo mapeamento é exclusivo do **MapStruct**.
* **Padrão HATEOAS Model Assembler & Paginação Type-Safe:**
  * Os assemblers no pacote `api.hateoas` implementam `RepresentationModelAssembler<EntidadeJPA, EntityModel<ResponseDTO>>`.
  * **Proibição de Hardcoding:** É estritamente proibido criar links manuais com Strings (`Link.of("/api/v1/...")`). Toda criação de links deve usar a reflexão nativa do Spring HATEOAS (`linkTo(methodOn(Controller.class)...)`). A passagem de argumentos `null` no `methodOn` para rotas de coleção é o padrão seguro e suportado no Spring HATEOAS 3.x+.
  * **Paginação Automática:** Endpoints paginados (`Page<EntidadeJPA>`) devem utilizar o `PagedResourcesAssembler` nativo do Spring nos Controllers (`pagedResourcesAssembler.toModel(page, assembler)`), preservando filtros e anexando links (`self`, `next`, `prev`, `first`, `last`) automaticamente.
  * O Assembler recebe a Entidade JPA, injeta internamente o Mapper do MapStruct, converte no DTO de resposta e anexa os links de hipermídia type-safe.
  * O Controller atua de forma limpa invocando diretamente `assembler.toModel(entity)` ou `pagedResourcesAssembler.toModel(...)`.
* **Configurações Centralizadas, Tipadas (`@ConfigurationProperties`) e Sem Mocks:**
  * É proibido o uso disperso de `@Value` pelo código.
  * Toda configuração deve ser centralizada em records tipados sob `@ConfigurationProperties(prefix = "...")`.
  * Proibido criar construtores secundários com valores mockados ou listas hardcoded dentro dos records de propriedades. O record deve refletir puramente o que vem do ambiente/YAML.
* **Tratamento Global de Erros com RFC 7807 (`ProblemDetail`):** Proibido criar DTOs manuais de erro genéricos. Toda resposta de erro REST da aplicação deve utilizar o padrão nativo **RFC 7807 (`ProblemDetail`)** fornecido pelo Spring Boot via `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler`.
* **Lombok sem Anemia:** Proibido `@Data` e `@Value`. Uso estrito de `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor` e `@AllArgsConstructor`.
* **Injeção de Dependências:** Proibido `@Autowired`. Injeção exclusivamente por construtor utilizando `private final` e anotação `@RequiredArgsConstructor` do Lombok.
* **Auditoria Centralizada:** Todas as novas tabelas de negócio devem obrigatoriamente estender a classe `AuditableEntity` para garantir rastreabilidade automática com `OffsetDateTime`.
* **Fuso Horário (Timezone):** Banco com `TIMESTAMPTZ`, Java com `OffsetDateTime` e `ApplicationTimeZoneConfig` travado em `America/Sao_Paulo`.
* **Consultas, Filtros Dinâmicos e Paginação Obrigatória:** 
  * É proibido o uso de *query methods* longos ou `@Query` manuais nos Repositories para filtros dinâmicos.
  * Toda busca com múltiplos filtros dinâmicos DEVE ser implementada utilizando **Spring Data JPA Specifications** (no pacote `domain.[subdominio].specification`).
  * Toda listagem de dados na API DEVE ser obrigatoriamente paginada (`Pageable`, `Page<T>`, `PagedModel`), evitando carregar coleções inteiras na memória e garantindo tempo de resposta constante.
* **Busca Textual Avançada:** Ao implementar *Specifications* para buscas de texto, deve-se obrigatoriamente utilizar a classe utilitária `PostgresSearchUtils` do projeto para lidar com normalizações e acentuação no banco via `unaccent`.
* **Mapeamento de Objetos:** Uso estrito de **MapStruct** com `unmappedTargetPolicy = ReportingPolicy.IGNORE` e `builder = @org.mapstruct.Builder(disableBuilder = true)`. Proibido `BeanUtils.copyProperties`.

---

## 4. Estrutura de Pastas e Separação de Camadas (Padrão Universal)

A arquitetura adota uma organização estrita, modular e limpa dividida por subdomínios funcionais:

```text
src/main/java/com/[empresa]/[modulo]/
├── api/                                # Camada Web / REST Externa
│   ├── controller/                     # Controladores REST divididos por subdomínio
│   ├── dto/                            # Transferência de Dados
│   │   ├── request/                    # Payloads de entrada (Records puros)
│   │   ├── response/                   # Payloads de saída (Records puros)
│   │   └── filter/                     # Objetos de filtro para busca avançada (Records puros)
│   ├── hateoas/                        # Assemblers e Hipermídia (RepresentationModel)
│   ├── mapper/                         # Mappers dedicados do MapStruct
│   └── validation/                     # Validadores customizados desacoplados (@Valid...)
├── domain/                             # Camada de Domínio e Negócio Rico
│   ├── common/                         # Utilitários compartilhados (AuditableEntity, PostgresSearchUtils, etc.)
│   └── [subdominio]/                   # Subdomínios funcionais do sistema
│       ├── entity/                     # Entidades JPA mapeadas
│       ├── model/                      # Records e objetos internos de domínio
│       ├── repository/                 # Repositórios Spring Data JPA (JpaRepository + JpaSpecificationExecutor)
│       ├── service/                    # Contratos/Interfaces de Serviço
│       │   └── impl/                   # Implementações concretas de Serviço (@Transactional)
│       └── specification/              # Specifications dinâmicas (Criteria API)
├── config/                             # Configurações Globais (Timezone, Jackson, Properties tipadas, CORS, OpenAPI)
└── exception/                          # Tratamento de Erros e Exceções
    ├── custom/                         # Exceções de negócio customizadas
    └── handler/                        # GlobalExceptionHandler com ProblemDetail (RFC 7807)
```

---

## 5. Normalização de Entrada de Dados (Trim Automático)
* **O Problema:** Usuários e integrações costumam enviar espaços em branco acidentais no início ou no fim de textos em formulários e JSONs (ex: `"  Texto   "`). Tratar isso manualmente em cada regra de negócio suja o código.
* **A Solução (Jackson Global):** A aplicação possui o `TrimStringDeserializer` registrado globalmente no Jackson.
* **Regra para a IA:** A IA **não deve** criar códigos manuais de limpeza de texto (como `string.trim()`) nos Services ou Controllers. O framework de serialização já faz isso de forma transparente na entrada.

---

## 6. Armazenamento de Arquivos (Storage)
* **Abstração S3/MinIO:** Todo e qualquer armazenamento de arquivo (imagens, PDFs, etiquetas, relatórios) deve utilizar o serviço de Storage compatível com S3 (ex: MinIO) configurado na infraestrutura.

---

## 7. Manutenção Viva da Documentação e Configurações
* **Variáveis de Ambiente:** Nenhuma senha ou credencial real deve ser commitada no repositório. Sempre que uma nova chave for adicionada aos arquivos de configuração, os respectivos templates de exemplo (`.env.dev.example` e `.env.prod.example`) DEVEM ser atualizados imediatamente.
* **Evolução do README:** O `README.md` principal é um documento vivo. Se a arquitetura mudar, novos comandos de build forem criados ou novas ferramentas forem adotadas, o README deve refletir essas mudanças no mesmo commit.
* **Configurações Locais na IDE:** A aplicação suporta perfis locais (`application-local.yml`) e o carregamento de variáveis via arquivos `.env` locais que são explicitamente ignorados pelo Git (`.gitignore`). Apenas arquivos de exemplo sobem para o repositório.

---

## 8. Ideias e Decisões Específicas deste Projeto (Hub Multiplataforma)
*As decisões abaixo foram adotadas especificamente para atender aos desafios de integração com múltiplos Marketplaces (Shopee, etc.):*

* **Segurança Fechada com Internal API Key:** Ingestão de webhooks protegida via cabeçalho `X-Internal-API-Key` contra acessos externos não autorizados.
* **Idempotência de Webhooks:** Verificação prévia por `order_sn` e status para evitar processamento redundante em retentativas de envio (*retry storms*).
* **Ordem de Execução Obrigatória para Pedidos COMPLETED (PostgreSQL Primeiro, Redis Depois):**
  1. **Atualização de Estado (PostgreSQL):** O pedido passa pelo processador de domínio para persistir e atualizar seu estado na tabela mestre (`orders_master`), aplicando as regras de negócio de domínio rico.
  2. **Enfileiramento no Redis (Delay Queue):** Somente **APÓS a confirmação de que o estado foi salvo com sucesso no banco de dados relacional**, o identificador do pedido (`PLATFORM:ORDER_SN`) é enviado para a fila de atraso no Redis (ZSet) para aguardar o tempo de espera configurado (ex: 120 minutos) da conciliação de Escrow. **O banco de dados é a fonte primária de verdade e o Redis atua apenas como orquestrador do agendamento posterior.**
* **Motor de Prova Real de Taxas (Matemática Fixa CPF):** Constantes oficiais de comissões e taxas para contas Pessoa Física no processador da Shopee, confrontando a matemática oficial com o extrato da API de Escrow e gravando `"has_divergence": true` no `metadata` JSONB caso haja divergências.
* **Persistência Híbrida (Relacional + JSONB):** O banco utiliza colunas relacionais fixas para campos financeiros e filtros críticos (`platform`, `shop_id`, `order_sn`, `status`, `reconciled`, `escrow_amount`) combinadas com a coluna curinga `metadata JSONB` (com índice `GIN`). Isso absorve itens, SKUs, variações, árvores completas de `income_details` e motivos de cancelamento (`cancel_reason`) sem necessidade de `ALTER TABLE`.
* **Precisão Monetária Máxima `DECIMAL(15,4)`:** Todos os valores financeiros de estimativa, frete cobrado e repasse líquido utilizam `DECIMAL(15,4)` no PostgreSQL e `BigDecimal` no Java para eliminar perdas em microcentavos de comissões e taxas fracionadas de e-commerce.
* **Event Store Imutável (`marketplace_raw_events`):** Todo webhook recebido de qualquer marketplace é gravado imediatamente em formato append-only com o payload JSON bruto completo antes de ser processado pelo sistema, garantindo auditoria, compliance e reprodutibilidade de eventos.
* **CORS Dinâmico por Ambiente:** Configuração de Cross-Origin centralizada no `WebCorsConfig` e vinculada ao record `OrderIntegrationProperties.CorsProperties`, permitindo ajuste fino de origens autorizadas via variável de ambiente `CORS_ALLOWED_ORIGINS`.

---

## 9. Gestão de Ambientes, `.env` e Commits Detalhados

* **Valores Oficiais nas Variáveis de Ambiente (`.env`):**
  * O `src/main/resources/application.yaml` **NÃO DEVE** conter valores padrão/fallbacks silenciosos na sintaxe `${VARIAVEL:valor_padrao}` para propriedades da aplicação.
  * Todas as variáveis devem ser mapeadas puramente como `${VARIAVEL}`.
  * Os valores reais oficiais de desenvolvimento residem exclusivamente no `.env.dev` (ignorado pelo Git).
* **Templates de Exemplo Genéricos (`.env.*.example`):**
  * O `.env.dev.example` e `.env.prod.example` servem exclusivamente como guias/templates para outros desenvolvedores ou servidores.
  * **Regra de Privacidade:** É expressamente proibido colocar dados reais de clientes, domínios específicos de produção (`dcriar.com`, tokens ativos) nesses arquivos de exemplo. Utilize sempre domínios de demonstração genéricos (ex: `https://painel.exemplo.com.br`, `https://api.exemplo.com.br`).
  * **Preservação de Seções:** Nunca delete seções não relacionadas (como MongoDB, Redis, n8n/ngrok) ao editar variáveis de ambiente.
* **Padrão Oficial de Mensagens de Commit (Git):**
  * Todos os commits devem seguir o padrão Conventional Commits com corpo explicativo em tópicos (`bullet points`), listando detalhadamente o que foi feito, por que foi feito e os resultados dos testes executados.
