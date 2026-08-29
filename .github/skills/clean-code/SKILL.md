---
name: clean-code
description: Regras de Clean Code, SOLID, JavaDoc, Injeção de Dependências, Lombok e separação de contratos. Use esta skill ao criar ou refatorar classes, serviços e componentes.
---

# Clean Code, SOLID & Boas Práticas Java/Spring

## 1. Documentação Obrigatória (JavaDoc) e Preservação Estrita
- Todas as classes, interfaces, enums e records devem conter JavaDoc descritivo no topo (`/** ... */`) explicando sua responsabilidade arquitetural em português.
- Métodos com regras de negócio, parâmetros de domínio ou validações específicas devem conter documentação técnica de seus parâmetros (`@param`), retornos (`@return`) e exceções (`@throws`).
- **Preservação Obrigatória:** Ao enriquecer classes/records existentes com anotações (ex: Swagger/OpenAPI `@Schema`, `@Tag`, `@Operation`), é **terminantemente proibido** remover ou reescrever blocos de JavaDocs pré-existentes. Apenas acrescente anotações ou ajuste cirurgicamente os `@param` caso a assinatura mude.

## 2. Princípio da Menor Alteração (Edições Cirúrgicas)
- É proibido refatorar ou reescrever arquivos inteiros quando a tarefa exigir apenas uma alteração pontual (como adicionar uma propriedade ou parâmetro de construtor em testes).
- Mantenha os diffs no Git sempre limpos, objetivos e sem ruído desnecessário.

## 3. Separação Estrita de Contrato e Implementação (Service vs. Service Impl)
- Toda regra de negócio de domínio deve ser definida como uma **interface de contrato** dentro do pacote `domain.[subdominio].service`.
- A implementação concreta correspondente deve residir obrigatoriamente dentro do pacote `domain.[subdominio].service.impl` (ex: `OrderMasterService` e `OrderMasterServiceImpl`).

## 4. Gerenciamento Transacional (`@Transactional`)
- As classes de implementação de serviço (`*ServiceImpl`) devem ser anotadas no topo da classe com `@Transactional(readOnly = true)`. Isso otimiza o consumo de memória e conexões no Hibernate para operações de leitura.
- Métodos que executam escrita, atualização ou exclusão de dados devem ser anotados explicitamente com `@Transactional` no método, garantindo atomicidade estrita.

## 5. DTO Records 100% Puros e Anêmicos
- Records nos pacotes de DTO (`api.dto.request`, `api.dto.response`, `api.dto.filter`) devem ser exclusivamente carreadores imutáveis de dados.
- É proibido colocar métodos de conversão de domínio (ex: `toEntity()`, `toCriteria()`, compact constructors de parsing manual) dentro dos DTOs.
- Toda conversão de/para DTOs deve ser delegada integralmente aos mappers do **MapStruct**.

## 6. Configurações Centralizadas, Tipadas (`@ConfigurationProperties`) e Sem Mocks
- **Proibido `@Value` disperso:** Toda propriedade vinda do `application.yml` ou variáveis de ambiente deve ser centralizada em classes ou records tipados anotados com `@ConfigurationProperties`.
- **Proibido Construtores Mockados:** Records de configuração não devem conter construtores secundários com valores hardcoded. Devem refletir puramente o que vem do ambiente/YAML.

## 7. Injeção de Dependências e Lombok
- **Proibido `@Autowired`:** Use injeção por construtor com atributos `private final`.
- Utilize `@RequiredArgsConstructor` do Lombok sobre a classe.
- **Proibido `@Data` e `@Value`:** Use anotações explícitas (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).

## 8. Rich Domain Model (Modelo de Domínio Rico)
- As entidades JPA não são anêmicas (meros getters/setters).
- Mudanças de estado, validações de integridade e cálculos devem residir nas entidades de domínio através de métodos expressivos.

## 9. Validações Desacopladas
- Validações de entrada de formulários e requisições devem ser desacopladas na camada `api.validation` com anotações customizadas e `ConstraintValidator`, mantendo controllers e services limpos.

## 10. Logs Estruturados
- É estritamente proibido o uso de `System.out.println` e `System.err.println`. Utilize logs adequados via anotação `@Slf4j` do Lombok (ex: `log.info(...)`, `log.warn(...)`, `log.error(...)`).

## 11. Padrão de Mensagens de Commit Detalhadas (Git)
- Todos os commits devem seguir a convenção de mensagens detalhadas:
  * Título: `tipo(escopo): descrição curta`
  * Corpo: tópicos (`bullet points`) explicando detalhadamente **o que** foi feito, **por que** foi feito e os **testes executados com sucesso**.
