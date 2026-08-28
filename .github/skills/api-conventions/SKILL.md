---
name: api-conventions
description: Regras para criacao de APIs REST, HATEOAS, Controllers, DTOs (records), Mapeamento (MapStruct), validacao de entrada, paginacao e tratamento de excecoes. Use esta skill sempre que trabalhar na camada web.
---

# Padrões de API e REST (Spring Boot)

## 1. Padrão REST, Paginação e Versionamento
- Todas as APIs devem adotar maturidade Richardson Nível 3 (HATEOAS).
- Controllers NUNCA retornam entidades JPA puras. Retorne sempre `ResponseEntity<Model>` mapeados via classes `*ModelAssembler` baseadas em `RepresentationModelAssemblerSupport`.
- **Paginação Obrigatória:** Todas as consultas e listagens de coleções na API DEVEM ser paginadas recebendo `Pageable` (com `@PageableDefault`) e retornando modelos paginados (`PagedModel<T>` ou `Page<T>`). É proibido retornar listas soltas (`List<T>`) sem paginação em endpoints de consulta.
- As rotas devem ser explícitas com status HTTP corretos (ex: `@ResponseStatus(HttpStatus.CREATED)` para POST).
- Utilize EXCLUSIVAMENTE a estratégia nativa de versionamento de API do Spring (por Header ou Media Type), sem inserir versões (como `/v1/`) manualmente no prefixo das URLs.

## 2. DTOs, Deserialização e Mapeamento
- DTOs devem ser separados em pacotes de `request` e `response`.
- Todo DTO de entrada (`*RequestDTO`) deve ser um Java `record` para garantir imutabilidade.
- Utilize deserializadores globais customizados (como sanitizadores de string e *trim* automático via `TrimStringDeserializer` no Jackson 3) para limpeza de strings de entrada antes do *payload binding*. É PROIBIDO fazer `.trim()` manual nos controllers ou services.
- O mapeamento entre Entidades e DTOs deve ser feito EXCLUSIVAMENTE via **MapStruct** (com `@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))`). É estritamente proibido o uso de `BeanUtils.copyProperties`.

## 3. Exceções e Tratamento de Erros
- NUNCA utilize exceções genéricas (`RuntimeException`, `IllegalArgumentException`) para regras de negócio.
- Crie exceções customizadas estendendo a hierarquia de exceções de negócio da aplicação.
- O sistema deve tratar os erros globalmente através de um `GlobalExceptionHandler`, retornando um DTO ou estrutura de erro padronizada do projeto. Não crie formatos de erro soltos nos controllers.

## 4. Documentação Viva (Swagger/OpenAPI 3)
- Controllers devem usar `@Tag` e `@Operation`.
- Campos dentro de DTOs/Records devem ter a anotação `@Schema` contendo a propriedade `example` com um valor real e condizente.
