# 🚀 Guia de Inicialização: Padrão Arquitetural e Guardrails

Este guia contém o passo a passo exato para replicar a infraestrutura de blindagem (Hooks, Skills e CI/CD) em qualquer novo projeto Java/Spring Boot.

## 📁 Passo 1: Criar a Estrutura de Pastas
No terminal, na raiz do novo projeto, crie a estrutura de diretórios necessária:

```bash
mkdir -p docs
mkdir -p .githooks
mkdir -p .github/hooks
mkdir -p .github/workflows
mkdir -p .github/skills/project-context
mkdir -p .github/skills/clean-code
mkdir -p .github/skills/api-conventions
mkdir -p .github/skills/jpa-conventions
```

## 📄 Passo 2: Copiar os Arquivos Base (Nesta Ordem)
Copie os arquivos do projeto base para as pastas recém-criadas:

1. **A Bíblia do Projeto:**
   - Copiar para: `docs/arquitetura.md`
2. **Skills da IA (Inteligência):**
   - Copiar para: `.github/skills/project-context/skill.md`
   - Copiar para: `.github/skills/clean-code/skill.md`
   - Copiar para: `.github/skills/api-conventions/skill.md`
   - Copiar para: `.github/skills/jpa-conventions/skill.md`
3. **O Cão de Guarda (Hook da IA):**
   - Copiar para: `.github/hooks/guardrails.sh`
   - Copiar para: `.github/hooks/guardrails.json` (Garante que a IDE chame o script)
4. **Blindagem Local (Git):**
   - Copiar para: `.githooks/pre-commit`
5. **Blindagem na Nuvem (CI/CD):**
   - Copiar para: `.github/workflows/guardrails.yml`

## ✏️ Passo 3: Ajustes Obrigatórios no Novo Projeto
Como todos os projetos seguirão a mesma stack (Spring Boot, MapStruct, Records, Jackson, etc.), a lógica arquitetural continua a mesma. Você só precisa ajustar os **nomes dos pacotes** e o **domínio**:

*   **`docs/arquitetura.md`**: Preencher a "Seção 1: Visão Geral do Sistema" com o domínio e as regras de negócio exclusivas do novo sistema.
*   **`.github/hooks/guardrails.sh`**:
    *   Onde houver checagem de pacote (ex: `domain/*.java` importando `.api.`), garantir que a string reflita o nome do pacote raiz do novo projeto (ex: `com.suaempresa.novo_projeto`).
*   **`.github/skills/*.md`**:
    *   Atualizar as menções aos pacotes base. Onde estava `com.seuprojeto.domain`, alterar para refletir o novo projeto.
    *   Certificar-se de que o nome das exceções customizadas (ex: `ErrorResponseDTO`) condiz com o padrão que será importado para este novo projeto.

## 🛡️ Passo 4: Ativação das Blindagens (Comandos)
Para que o Git local do desenvolvedor e o sistema operacional respeitem as travas, execute estes comandos na raiz do projeto:

**1. Dar permissão de execução para os scripts:**
```bash
chmod +x .github/hooks/guardrails.sh
chmod +x .githooks/pre-commit
```

**2. Ativar o Git Hook localmente:**
```bash
git config core.hooksPath .githooks
```
*(Isso avisa ao Git para parar de procurar hooks na pasta oculta `.git/hooks` e passar a usar a pasta `.githooks` que está versionada no projeto).*

## ✅ Passo 5: Validação Final
- [ ] O CI do GitHub Actions está habilitado no repositório?
- [ ] O `git config core.hooksPath .githooks` rodou sem erros?
- [ ] O agente de IA está configurado para ler o `guardrails.json` na raiz?

Feito isso, o projeto está 100% blindado contra "código sujo" gerado por humanos ou IAs!