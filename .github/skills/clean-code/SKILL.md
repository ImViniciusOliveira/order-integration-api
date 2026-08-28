---
name: clean-code
description: Regras de Clean Code, SOLID, JavaDoc, Injeção de Dependências, Lombok e separação de contratos. Use esta skill ao criar ou refatorar classes, serviços e componentes.
---

# Clean Code, SOLID & Boas Práticas Java/Spring

## 1. Documentação Obrigatória (JavaDoc)
- Todas as classes, interfaces, enums e records devem conter JavaDoc descritivo no topo (`/** ... */`) explicando sua responsabilidade arquitetural.
- Métodos com regras de negócio, parâmetros de domínio ou validações específicas devem conter documentação técnica de seus parâmetros (`@param`), retornos (`@return`) e exceções (`@throws`).

## 2. Separação Estrita de Contrato e Implementação (Service vs. Service Impl)
- Toda regra de negócio de domínio deve ser definida como uma **interface de contrato** dentro do pacote `domain.[subdominio].service`.
- A implementação concreta correspondente deve residir obrigatoriamente dentro do pacote `domain.[subdominio].service.impl` (ex: `OrderMasterService` e `OrderMasterServiceImpl`).

## 3. Configurações Centralizadas e Fortemente Tipadas (`@ConfigurationProperties`)
- **Proibido `@Value` disperso:** Toda propriedade vinda do `application.yml` ou variáveis de ambiente deve ser centralizada em classes ou records tipados anotados com `@ConfigurationProperties`.
- Garante validação no startup, autocompletion e facilidade de recarga de configuração.

## 4. Injeção de Dependências e Lombok
- **Proibido `@Autowired`:** Use injeção por construtor com atributos `private final`.
- Utilize `@RequiredArgsConstructor` do Lombok sobre a classe.
- **Proibido `@Data` e `@Value`:** Use anotações explícitas (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## 5. Rich Domain Model (Modelo de Domínio Rico)
- As entidades JPA não são anêmicas (meros getters/setters).
- Mudanças de estado, validações de integridade e cálculos devem residir nas entidades de domínio através de métodos expressivos.

## 6. Validações Desacopladas
- Validações de entrada de formulários e requisições devem ser desacopladas na camada `api.validation` com anotações customizadas e `ConstraintValidator`, mantendo controllers e services limpos.

## 7. Logs e Manutenção
- É estritamente proibido o uso de `System.out.println` e `System.err.println`. Utilize logs adequados via anotação `@Slf4j` do Lombok (ex: `log.info(...)`, `log.warn(...)`, `log.error(...)`).
