---
name: api-conventions
description: Regras para endpoints REST, DTO records, MapStruct, HATEOAS, RFC 7807 ProblemDetail, validações e serialização. Use esta skill ao criar ou alterar Controllers, DTOs, Mappers e Handlers de erro.
---

# Padrões de API REST, DTOs, Mappers, HATEOAS & Error Handling

## 1. Estrutura Modular da Camada Web (`api`)
A camada de API reside sob o pacote `api`:
- `api.controller.[subdominio]`: Controladores REST.
- `api.dto.request`: Records de entrada.
- `api.dto.response`: Records de saída.
- `api.dto.filter`: Records de critérios de busca avançada.
- `api.hateoas`: Assemblers e modelos de hipermídia (`PagedModel`, `CollectionModel`, `EntityModel`).
- `api.mapper`: Interfaces de mapeamento MapStruct.
- `api.validation`: Anotações customizadas e `ConstraintValidator`.

## 2. Tratamento Global de Erros com RFC 7807 (`ProblemDetail`)
- **Proibido DTOs manuais de erro:** Não crie classes manuais genéricas de resposta de erro.
- Toda resposta de erro HTTP (400, 404, 422, 500) deve utilizar o padrão **RFC 7807 (`org.springframework.http.ProblemDetail`)** fornecido nativamente pelo Spring Boot.
- Implemente um `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler` e personalize os retornos via `problemDetail.setProperty(...)`.

## 3. DTO Records & MapStruct
- Entidades JPA nunca são expostas diretamente nos controllers.
- Use `record` do Java para todos os DTOs de Request, Response e Filter (100% puros e sem lógica de negócio).
- Use **MapStruct** para conversão entre DTOs, Critérios e Entidades, configurado obrigatoriamente com `disableBuilder = true`:
  ```java
  @Mapper(
      componentModel = MappingConstants.ComponentModel.SPRING,
      unmappedTargetPolicy = ReportingPolicy.IGNORE,
      builder = @org.mapstruct.Builder(disableBuilder = true)
  )
  public interface GenericMapper {
      // Metodos de mapeamento
  }
  ```

## 4. Diretrizes de HATEOAS Type-Safe e Paginação Limpa

### 4.1. Proibição de Hardcoding com `Link.of`
É expressamente proibido construir links injetando rotas estáticas em formato de String (ex: `Link.of("/api/v1/orders/123")`).
* **Motivo:** Quebra a refatoração automática da IDE e gera bugs silenciosos em produção caso rotas ou versionamentos sejam alterados.

### 4.2. Construção Reflexiva Type-Safe com `linkTo(methodOn(...))`
Utilize sempre a reflexão nativa do Spring HATEOAS:
```java
// Link para recurso individual
linkTo(methodOn(OrderMasterController.class).findById(entity.getId())).withSelfRel();

// Link para coleções / filtros (passagem de null em methodOn é o padrão oficial seguro do Spring HATEOAS 3.x+)
linkTo(methodOn(OrderMasterController.class).searchOrders(null, null)).withRel("collection");
```

### 4.3. Paginação Automática via `PagedResourcesAssembler`
Nos Controllers com endpoints paginados (`Page<T>`), nunca construa links de paginação manualmente.
Injete o `PagedResourcesAssembler<EntidadeJPA>` e delegue a geração do `PagedModel`:
```java
@GetMapping
public ResponseEntity<PagedModel<EntityModel<OrderMasterResponse>>> searchOrders(
        @ModelAttribute OrderFilterRequest filter,
        Pageable pageable) {
    OrderFilterCriteria criteria = orderMasterMapper.toCriteria(filter);
    Page<OrderMaster> ordersPage = orderMasterService.searchOrders(criteria, pageable);
    PagedModel<EntityModel<OrderMasterResponse>> pagedModel = pagedResourcesAssembler.toModel(ordersPage, orderMasterModelAssembler);
    return ResponseEntity.ok(pagedModel);
}
```
* O Spring lerá automaticamente a requisição atual (`HttpServletRequest`) e anexará os links `self`, `next`, `prev`, `first` e `last`, preservando todos os parâmetros de busca aplicados.

## 5. Normalização Global de Strings (Trim)
- O Jackson 3 possui o deserializer global `TrimStringDeserializer` ativo.
- Não faça trim manual em controllers ou services.
