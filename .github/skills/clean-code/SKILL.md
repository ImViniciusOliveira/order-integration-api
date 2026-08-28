---
name: clean-code
description: Regras universais de Clean Code, SOLID, isolamento de camadas, restrições do Lombok, JavaDoc e validação limpa. A IA deve usar esta skill continuamente para qualquer geracao de codigo Java.
---

# Clean Code, SOLID e Boas Práticas

## 0. Prioridade do Projeto
- Antes de aplicar qualquer regra genérica de código, considere primeiro o contexto do projeto, a skill `project-context` e o arquivo `docs/arquitetura.md`.
- Em caso de dúvida entre uma prática "padrão" e uma decisão já definida no projeto, a decisão do projeto sempre prevalece.

## 1. Documentação (JavaDoc Obrigatório)
- Todas as classes, interfaces e records de negócio/domínio/serviço devem conter JavaDoc no topo (`/** ... */`) explicando seu propósito arquitetural e responsabilidade no sistema.
- Métodos de domínio rico ou com lógica de negócio relevante devem conter tags descritivas (`@param`, `@return`) explicando as regras aplicadas.

## 2. SOLID
- **S**ingle Responsibility: cada classe deve ter um único motivo para mudar.
- **O**pen/Closed: prefira estender comportamento sem alterar a lógica central já validada.
- **L**iskov Substitution: implementações concretas devem poder substituir a abstração sem quebrar o contrato.
- **I**nterface Segregation: mantenha interfaces pequenas e específicas, evitando dependências desnecessárias.
- **D**ependency Inversion: dependa de abstrações, não de implementações concretas.

## 3. Isolamento de Camadas (Strict Separation)
- A camada de domínio (`domain`) é totalmente isolada e NUNCA deve importar classes da camada web/api (`api` / `controller`).
- O fluxo de dados é estrito: O `Controller` só pode chamar o `Service`. É PROIBIDO o `Controller` injetar ou acessar o `Repository` diretamente.

## 4. Lombok e Injeção de Dependências
- É PROIBIDO o uso das anotações `@Data` e `@Value` do Lombok. Declare apenas o necessário de forma explícita (`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`).
- É PROIBIDO o uso de `@Autowired` em atributos. Utilize injeção via construtor declarando atributos como `private final` junto com `@RequiredArgsConstructor`.

## 5. Clean Validation (Desacoplada)
- É estritamente PROIBIDO poluir os DTOs (`records`) com múltiplas anotações do Bean Validation (como `@NotNull`, `@NotBlank`, `@Size`, etc. empilhadas nos campos).
- **Padrão Obrigatório de Validação:** O projeto utiliza validação totalmente desacoplada através de validadores customizados:
  1. Criar uma anotação customizada no pacote de validação (ex: `@ValidRequest`).
  2. Criar a classe validadora correspondente implementando a interface `ConstraintValidator` do Spring.
  3. Concentrar toda a lógica de checagem dos campos (verificação de nulos, tamanhos, formatação e regras de unicidade) dentro do método `isValid` desta classe validadora.
  4. O DTO (`record`) importará e utilizará apenas esta anotação customizada, mantendo a sua estrutura de dados 100% limpa e focada apenas no tráfego de dados.

## 6. Logs e Manutenção
- É estritamente proibido o uso de `System.out.println` e `System.err.println`. Utilize logs adequados via anotação `@Slf4j` do Lombok (ex: `log.info(...)`, `log.error(...)`).
