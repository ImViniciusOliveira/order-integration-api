# Arquitetura e Decisões de Projeto (`arquitetura.md`)

Este documento descreve as diretrizes arquiteturais, regras de infraestrutura e decisões de negócio adotadas neste projeto. **Qualquer IA ou desenvolvedor atuando neste repositório deve ler e seguir estas regras antes de propor novas funcionalidades.**

## 1. Visão Geral do Sistema
*(Preencha aqui o contexto do seu sistema. Ex: Este é um sistema de gestão X. O domínio principal gira em torno de módulos de Vendas, Usuários e Estoque.)*

## 2. Topologia e Infraestrutura (Docker e Compose)
O projeto adota uma estrutura flexível de containers para desenvolvimento e produção, baseada na combinação de arquivos Compose:

* **Modo de Desenvolvimento (Backend na IDE):** Utiliza-se apenas o arquivo `docker-compose.dev.yml` (e/ou ignorando o build do backend) para subir **exclusivamente** as dependências de infraestrutura (PostgreSQL, MinIO, Redis, etc.). Neste cenário, a aplicação Spring Boot é rodada diretamente pela IDE, permitindo debug ágil e *hot-reload*.
* **Modo de Desenvolvimento Full (Docker + Override):** Ao executar o ambiente utilizando o `docker-compose.dev.yml` em conjunto com o `docker-compose.override.yml`, a stack completa é levantada, **incluindo o próprio backend rodando via container**. Isso serve para validar localmente o comportamento real da aplicação "containerizada" antes do deploy.
* **Ambiente de Produção:** O arquivo `docker-compose.prod.yml` é estritamente isolado e contém a configuração otimizada e segura de fato para o deploy em produção.
* **Atualização Obrigatória:** Qualquer nova dependência externa adicionada ao sistema (ex: um novo banco ou sistema de fila) deve ter seu container correspondente mapeado e sincronizado nos arquivos de dev e prod.

## 3. Persistência, Auditoria e Consultas (Padrões do Projeto)
A aplicação possui uma infraestrutura base (`domain.common`) que DEVE ser reaproveitada em todos os novos módulos:

* **Auditoria Centralizada:** Todas as novas tabelas de negócio devem obrigatoriamente estender a classe `AuditableEntity` para garantir rastreabilidade automática.
* **Fuso Horário (Timezone):** O controle de datas é centralizado e travado via `ApplicationTimeZoneConfig`. Não altere configurações de data pontualmente.
* **Consultas e Filtros Dinâmicos (Specifications):** É proibido o uso de *query methods* longos ou `@Query` complexas nos Repositories. Toda busca com múltiplos filtros dinâmicos DEVE ser implementada utilizando **Spring Data JPA Specifications** (na pasta `repository/spec`).
* **Busca Textual Avançada:** Ao implementar *Specifications* para buscas de texto, a IA deve obrigatoriamente utilizar a classe utilitária `PostgresSearchUtils` do projeto para lidar com normalizações e acentuação no banco.
* **Validação de Unicidade:** Para validar se um dado já existe no banco (ignorando acentos e case-sensitive), utilize a implementação baseada no `PostgresNormalizedUniquenessChecker` e na extensão `unaccent` do PostgreSQL.
## 4. Normalização de Entrada de Dados (Trim Automático)
* **O Problema:** Usuários costumam digitar espaços em branco acidentais no início ou no fim de textos em formulários (ex: `"  Produto X   "`). Tratar isso manualmente em cada regra de negócio suja o código.
* **A Solução (Jackson Global):** A aplicação possui um componente global de infraestrutura chamado `TrimStringDeserializer` configurado no Jackson.
* **Como funciona na prática:** Antes mesmo de o dado JSON da API atingir os DTOs (`records`) ou as regras de negócio, o Jackson intercepta automaticamente todas as Strings, remove os espaços indesejados das pontas (*trim*) e entrega o dado limpo.
* **Regra para a IA:** A IA **não deve** criar códigos manuais de limpeza de texto (como `string.trim()`) nos Services ou Controllers. O framework de serialização já faz isso de forma transparente na entrada.
## 5. Armazenamento de Arquivos (Storage)

* **Abstração S3/MinIO:** Todo e qualquer armazenamento de arquivo (imagens, PDFs, relatórios) deve utilizar o serviço de Storage compatível com S3 (ex: MinIO) configurado na infraestrutura.

## 6. Manutenção Viva da Documentação e Configurações
* **Variáveis de Ambiente:** Nenhuma senha ou credencial real deve ser commitada no repositório. Sempre que uma nova chave for adicionada aos arquivos de configuração (como `application-dev.yml`, `application-prod.yml` ou arquivos base), os respectivos templates de exemplo (`.env.dev.example` e `.env.prod.example`) DEVEM ser atualizados imediatamente.
* **Evolução do README:** O `README.md` principal é um documento vivo. Se a arquitetura mudar, novos comandos de build forem criados ou novas ferramentas forem adotadas, o README deve refletir essas mudanças no mesmo commit.

## 7. Configurações Locais e Variáveis de Ambiente na IDE
* **O Problema:** Em ambiente de desenvolvimento local (rodando a aplicação diretamente na IDE), precisamos injetar credenciais ou parâmetros de conexão específicos sem alterar os arquivos de configuração globais.
* **A Solução (Arquivos Locais e `.env`):** A aplicação suporta perfis locais (como `application-local.yml`) e o carregamento de variáveis via arquivos `.env` locais que são explicitamente ignorados pelo Git (`.gitignore`).
* **Regra para a IA:**
  1. Nenhum arquivo de configuração local contendo senhas reais ou dados sensíveis (como `.env` ou `application-local.yml`) deve ser commitado no repositório.
  2. Sempre utilize arquivos de exemplo (como `.env.dev.example` ou `.env.prod.example`) para guiar quais variáveis a aplicação espera receber.
  3. A IA deve orientar o desenvolvedor a configurar essas variáveis diretamente na aba de "Environment Variables" da sua IDE (IntelliJ ou VS Code) caso necessário para testes locais.