---
name: jpa-conventions
description: Regras para banco de dados, JPA, PostgreSQL, Rich Domain Model, Timezone, Paginação, Flyway e infraestrutura. Use esta skill ao criar Entidades, Repositórios, Specifications ou migrações.
---

# Padrões de Persistência, JPA e Banco de Dados

## 1. Banco de Dados e Migrations (Flyway)
- A evolução do schema do banco de dados é feita exclusivamente via Flyway (`db/migration/`).
- Nunca dependa da geração automática do Hibernate (o `ddl-auto` deve ser `validate` ou `none`, NUNCA `update`).
- Ao necessitar de buscas que ignorem acentos no banco (PostgreSQL), utilize extensões nativas como o `unaccent`.

## 2. Localização de Pacotes por Subdomínio
- Entidades JPA devem residir no pacote correspondente do seu subdomínio: `domain.[subdominio].entity`.
- Repositórios Spring Data JPA devem residir em: `domain.[subdominio].repository`.
- Specifications dinâmicas devem residir em: `domain.[subdominio].specification`.

## 3. Modelagem de Entidades e Rich Domain Model (JPA)
- Entidades JPA não devem ser anêmicas. As alterações de estado e regras de integridade devem ser realizadas via métodos expressivos de domínio.
- Todas as entidades de negócio devem estender `AuditableEntity` com suporte a `TIMESTAMPTZ` no banco e `OffsetDateTime` no Java para rastreabilidade com controle estrito de fuso horário (`America/Sao_Paulo`).
- Mapeamento JSON/JSONB no PostgreSQL deve utilizar a anotação `@JdbcTypeCode(SqlTypes.JSON)` do Hibernate.

## 4. Consultas, Filtros e Paginação Obrigatória (Specifications)
- É PROIBIDO o uso de *query methods* gigantescos no Repositório ou `@Query` manuais para filtros dinâmicos.
- Repositórios devem estender `JpaRepository<T, ID>` e `JpaSpecificationExecutor<T>`.
- Buscas dinâmicas devem ser implementadas utilizando o padrão **Spring Data JPA Specifications** com `JpaSpecificationExecutor`, organizadas em `domain.[subdominio].specification`.
- **Paginação Obrigatória:** Todas as consultas de listagem com Specifications devem receber `Pageable` e retornar `Page<T>`, garantindo consultas com `LIMIT` e `OFFSET` no banco.
- Para buscas textuais dinâmicas com normalização e acentuação, utilize `PostgresSearchUtils` (`containsNormalized`).

## 5. Tipos Monetários
- Valores monetários utilizam sempre `DECIMAL(15,4)` no PostgreSQL e `BigDecimal` no Java para máxima precisão decimal.

## 6. Validação de Unicidade
- Para validações de atributos únicos no banco (ex: códigos, chaves, nomes), utilize checagens normalizadas no banco que ignorem variações de caixa e acentos via `PostgresNormalizedUniquenessChecker`.
