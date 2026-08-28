---
name: jpa-conventions
description: Regras para banco de dados, JPA, PostgreSQL, Rich Domain Model, Timezone, Paginação, Flyway e infraestrutura. Use esta skill ao criar Entidades (Models), Repositories, Queries ou migrações.
---

# Padrões de Persistência e Banco de Dados

## 1. Banco de Dados e Migrations (Flyway)
- A evolução do schema do banco de dados é feita exclusivamente via Flyway (`db/migration/`).
- Nunca dependa da geração automática do Hibernate (o `ddl-auto` deve ser `validate` ou `none`, NUNCA `update`).
- Ao necessitar de buscas que ignorem acentos no banco (PostgreSQL), utilize extensões nativas como o `unaccent`.

## 2. Modelagem de Entidades e Rich Domain Model (JPA)
- Entidades JPA não devem ser anêmicas (sacos de getters e setters). As alterações de estado e regras de integridade devem ser realizadas via métodos expressivos de domínio (ex: `conciliarEscrow(...)`, `provisionarEstimativa(...)`, `ativar()`).
- Para novas tabelas, utilize a estratégia de chave primária condizente com a modelagem do schema (ex: `BIGINT GENERATED ALWAYS AS IDENTITY` ou `UUID`), conforme o DDL do Flyway.
- Todas as Entidades de negócio devem estender `AuditableEntity` com suporte a `TIMESTAMPTZ` no banco e `OffsetDateTime` no Java para rastreabilidade com controle estrito de fuso horário.
- Mapeamento JSON/JSONB no PostgreSQL deve utilizar a anotação `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate.

## 3. Consultas, Filtros e Paginação (Specifications)
- É PROIBIDO o uso de *query methods* gigantescos no Repositório ou `@Query` com SQL nativo/JPQL para filtros dinâmicos.
- Buscas dinâmicas devem ser implementadas utilizando o padrão **Spring Data JPA Specifications** com `JpaSpecificationExecutor`, organizadas em pacotes de especificações por domínio (`domain.specification`).
- **Paginação Obrigatória:** Todas as consultas de listagem com Specifications devem receber `Pageable` e retornar `Page<T>`, garantindo consultas com `LIMIT` e `OFFSET` no banco.
- Para buscas textuais dinâmicas, utilize `PostgresSearchUtils` para normalização com `unaccent` e case-insensitive.

## 4. Validação de Unicidade
- Para validações de atributos únicos no banco (ex: e-mail, SKU, nome), utilize checagens normalizadas no banco que ignorem variações de caixa e acentos via `PostgresNormalizedUniquenessChecker`.
