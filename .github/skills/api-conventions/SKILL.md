---
name: api-conventions
description: Regras para endpoints REST, DTO records, MapStruct, HATEOAS, RFC 7807 ProblemDetail, validações e serialização. Use esta skill ao criar ou alterar Controllers, DTOs, Mappers e Handlers de erro.
---

# Padrões de API REST, DTOs, Mappers, HATEOAS & Error Handling

## 1. Estrutura Modular da Camada Web (`api`)
A camada de API reside sob o pacote `api`:
- `api.controller.[subdominio]`: Controladores REST.
- `api.dto.request`: Records/classes de entrada.
- `api.dto.response`: Records/classes de saída.
- `api.dto.filter`: Records de critérios de busca avançada.
- `api.hateoas`: Assemblers e modelos de hipermídia (`PagedModel`, `RepresentationModel`).
- `api.mapper`: Interfaces de mapeamento MapStruct.
- `api.validation`: Anotações customizadas e `ConstraintValidator`.

## 2. Tratamento Global de Erros com RFC 7807 (`ProblemDetail`)
- **Proibido DTOs manuais de erro:** Não crie classes manuais genéricas de resposta de erro.
- Toda resposta de erro HTTP (400, 404, 422, 500) deve utilizar o padrão **RFC 7807 (`org.springframework.http.ProblemDetail`)** fornecido nativamente pelo Spring Boot.
- Implemente um `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler` e personalize os retornos via `problemDetail.setProperty(...)`.

## 3. DTO Records & MapStruct
- Entidades JPA nunca são expostas diretamente nos controllers.
- Use `record` do Java para todos os DTOs de Request, Response e Filter.
- Use **MapStruct** para conversão entre DTOs e Entidades, configurado obrigatoriamente com `disableBuilder = true`:
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

## 4. Paginação Obrigatória e HATEOAS
- Todos os endpoints de listagem devem aceitar `Pageable` e retornar respostas paginadas.
- Utilize HATEOAS com `PagedModel` para fornecer links de navegação (`next`, `prev`, `self`).

## 5. Normalização Global de Strings (Trim)
- O Jackson 3 possui o deserializer global `TrimStringDeserializer` ativo.
- Não faça trim manual em controllers ou services.
