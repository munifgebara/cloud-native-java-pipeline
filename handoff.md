# Handoff Completo - Stella / cloud-native-java-pipeline

## Contexto Geral

Estamos trabalhando no repositório `cloud-native-java-pipeline`, que hoje representa o projeto **Stella**, um sistema de **inventário pessoal**.

O contexto correto do projeto foi confirmado e atualizado:
- não é mais o contexto antigo de “pagadoria”
- o domínio atual é inventário pessoal
- o backend é em Spring Boot
- o frontend é Angular
- autenticação com Keycloak/OAuth2/JWT
- persistência com PostgreSQL
- há Docker Compose para ambiente local
- há Kubernetes e GitHub Actions no fluxo do projeto
- observabilidade com Actuator e integração prevista com Prometheus/Grafana

Também já foi confirmado no histórico recente:
- o Stella já possui CRUD de `pessoas`
- o Stella já possui CRUD de `categorias`
- já existe dashboard inicial
- já existe i18n
- a documentação foi reorganizada para portfólio e material didático

## Cuidados Operacionais Definidos com o Usuário

O usuário pediu explicitamente estas regras de trabalho:
- **nunca trabalhar diretamente na `main`**
- sempre partir de branch própria
- se houver sujeira/local divergence na `main`, descartar e alinhar com `origin/main`
- não alterar configuração global de Git do usuário
- neste repositório local, os commits devem sair com a identidade do bot

Foi configurado anteriormente neste repo:
- `git config --local user.name "MunIAgebot"`
- `git config --local user.email "muniagebot@gmail.com"`

Também foi resolvida a questão de autoria da PR:
- commits e push devem sair com `MunIAgebot`
- PRs devem ser abertas com `gh` autenticado como `muniagebot`
- não usar a integração que abre PR como `munifgebara`, porque isso já gerou PR com autoria errada no passado

## Situação do Ambiente Windows Anterior

O ambiente Windows anterior começou a apresentar instabilidade no app do Codex durante autorizações, com erro do tipo:
- `Unable to find Electron app ... type=action&action=...`
- isso aparenta ser um problema do app/handler de autorização do Codex Desktop no Windows

Além disso:
- o `mvnw.cmd` falhou localmente no Windows com erro interno no wrapper PowerShell
- por isso não foi possível extrair percentual exato de cobertura de testes naquele ambiente
- o usuário decidiu preservar o Windows porque usa essa máquina para outros projetos legados
- o trabalho deve continuar no Linux

## Estado do Repositório e do Backlog

Foi feita uma reavaliação do backlog com base na `main` atualizada.

### O que já existe no Stella
- `pessoas`
- `categorias`
- dashboard inicial
- i18n
- estrutura backend/frontend consolidada para CRUD

### Ajuste importante de backlog
Foi confirmado que a modelagem do inventário deve considerar:
- **item mestre**
- **instância do item**

Exemplo:
- Item mestre: “Notebook Dell Latitude 5440”
- Instâncias: patrimônio `NB-001`, `NB-002`, etc.

Ou seja:
- imagem pertence ao **item mestre**
- movimentações pertencem à **instância**
- empréstimo/devolução pertencem à **instância**
- localização atual pertence à **instância**

Também foi decidido:
- categorias terão **ícones predefinidos**
- imagens dos itens serão armazenadas em **MinIO**
- não usar imagem por instância
- a imagem é do item mestre

## Issues Criadas no GitHub

Foram criadas as issues do backlog principal do inventário:

- `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
- `#27` Criar cadastro de item mestre do inventário
- `#28` Adicionar ícones predefinidos ao cadastro de categorias
- `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
- `#30` Adicionar imagem principal ao cadastro de item mestre
- `#31` Criar cadastro de instâncias de item
- `#32` Criar listagem e filtros de itens e instâncias
- `#33` Definir status operacional das instâncias de item
- `#34` Implementar movimentações de entrada de instâncias
- `#35` Implementar movimentações de saída de instâncias
- `#36` Implementar transferência de instâncias entre locais
- `#37` Criar histórico consolidado da instância do item
- `#38` Implementar empréstimo de instâncias para pessoas
- `#39` Implementar devolução de instâncias emprestadas
- `#40` Validar regras de negócio de inventário, movimentação e empréstimo
- `#41` Expandir o dashboard com indicadores do inventário
- `#42` Adicionar testes automatizados ao módulo de inventário

## Duplicidade Resolvida no Backlog Antigo

Foi identificada duplicidade entre:
- `#15`
- `#19`

A decisão tomada foi:
- manter `#19` como issue canônica
- fechar `#15` como duplicata de `#19`

Isso já foi feito no GitHub.

## Reordenação Final do Backlog

A ordem final recomendada de implementação ficou assim:

1. `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
2. `#27` Criar cadastro de item mestre do inventário
3. `#31` Criar cadastro de instâncias de item
4. `#33` Definir status operacional das instâncias de item
5. `#32` Criar listagem e filtros de itens e instâncias
6. `#34` Implementar movimentações de entrada de instâncias
7. `#35` Implementar movimentações de saída de instâncias
8. `#36` Implementar transferência de instâncias entre locais
9. `#38` Implementar empréstimo de instâncias para pessoas
10. `#39` Implementar devolução de instâncias emprestadas
11. `#37` Criar histórico consolidado da instância do item
12. `#40` Validar regras de negócio de inventário, movimentação e empréstimo
13. `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
14. `#30` Adicionar imagem principal ao cadastro de item mestre
15. `#41` Expandir o dashboard com indicadores do inventário
16. `#28` Adicionar ícones predefinidos ao cadastro de categorias
17. `#42` Adicionar testes automatizados ao módulo de inventário

## Dependências Entre Issues

Dependências explícitas definidas:

- `#26` sem dependências
- `#27` sem dependências, aproveitando o cadastro de categorias já existente
- `#31` depende de `#27`
- `#33` depende de `#31`
- `#32` depende de `#27`, `#31` e idealmente `#33`
- `#34` depende de `#26`, `#31`, `#33`
- `#35` depende de `#26`, `#31`, `#33`
- `#36` depende de `#26`, `#31`, `#33`, `#34` e `#35`
- `#38` depende de `#31`, `#33` e do módulo de `pessoas` já existente
- `#39` depende de `#38`
- `#37` depende de `#34`, `#35`, `#36`, `#38`, `#39`
- `#40` depende de `#33`, `#34`, `#35`, `#36`, `#38`, `#39`
- `#29` pode evoluir em paralelo, mas é pré-requisito direto de `#30`
- `#30` depende de `#27` e `#29`
- `#41` depende de massa crítica do módulo de inventário, idealmente após `#27`, `#31`, `#33`, `#38`
- `#28` pode ser feita em paralelo, não bloqueia o domínio principal
- `#42` depende do bloco principal implementado, para cobrir locais, itens, instâncias, movimentações e empréstimos

## Organização do GitHub Já Aplicada

Foi organizada a visão do backlog no GitHub para facilitar a escolha de tarefas por qualquer DEV que entrar no projeto.

### Milestones criadas
- `Fase 1 - Nucleo do Inventario`
- `Fase 2 - Movimentacoes`
- `Fase 3 - Emprestimos e Regras`
- `Fase 4 - Midia com MinIO`
- `Fase 5 - Dashboard e Qualidade`

### Labels criadas
- `phase:1`
- `phase:2`
- `phase:3`
- `phase:4`
- `phase:5`
- `ready`
- `roadmap`

### Aplicação de labels e milestones
As issues do backlog `#26` a `#42` foram associadas às fases correspondentes.

Foram marcadas como `ready` as issues que hoje podem ser puxadas sem dependências abertas:
- `#26`
- `#27`
- `#29`
- `#28`

### Issue índice criada
Foi criada uma issue índice para servir de guia:
- `#43` Roadmap do modulo de inventario

Também surgiu duplicada uma segunda issue índice:
- `#44` Roadmap do modulo de inventario

A decisão foi:
- manter `#43`
- fechar `#44` como duplicata

## Texto já preparado para README

Já existe um texto formatado em Markdown para o roadmap do inventário, caso precise reaproveitar no Linux. O conteúdo era:

```md
## Roadmap de Implementação do Módulo de Inventário

A implementação deve priorizar sempre a menor fase aberta e, dentro dela, as issues marcadas como `ready`.

### Ordem Recomendada

1. `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
2. `#27` Criar cadastro de item mestre do inventário
3. `#31` Criar cadastro de instâncias de item
4. `#33` Definir status operacional das instâncias de item
5. `#32` Criar listagem e filtros de itens e instâncias
6. `#34` Implementar movimentações de entrada de instâncias
7. `#35` Implementar movimentações de saída de instâncias
8. `#36` Implementar transferência de instâncias entre locais
9. `#38` Implementar empréstimo de instâncias para pessoas
10. `#39` Implementar devolução de instâncias emprestadas
11. `#37` Criar histórico consolidado da instância do item
12. `#40` Validar regras de negócio de inventário, movimentação e empréstimo
13. `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
14. `#30` Adicionar imagem principal ao cadastro de item mestre
15. `#41` Expandir o dashboard com indicadores do inventário
16. `#28` Adicionar ícones predefinidos ao cadastro de categorias
17. `#42` Adicionar testes automatizados ao módulo de inventário

### Implementação por Fases

#### Fase 1 - Núcleo do Inventário
- `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
- `#27` Criar cadastro de item mestre do inventário
- `#31` Criar cadastro de instâncias de item
- `#33` Definir status operacional das instâncias de item
- `#32` Criar listagem e filtros de itens e instâncias

#### Fase 2 - Movimentações
- `#34` Implementar movimentações de entrada de instâncias
- `#35` Implementar movimentações de saída de instâncias
- `#36` Implementar transferência de instâncias entre locais

#### Fase 3 - Empréstimos e Regras
- `#38` Implementar empréstimo de instâncias para pessoas
- `#39` Implementar devolução de instâncias emprestadas
- `#37` Criar histórico consolidado da instância do item
- `#40` Validar regras de negócio de inventário, movimentação e empréstimo

#### Fase 4 - Mídia com MinIO
- `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
- `#30` Adicionar imagem principal ao cadastro de item mestre

#### Fase 5 - Dashboard e Qualidade
- `#41` Expandir o dashboard com indicadores do inventário
- `#28` Adicionar ícones predefinidos ao cadastro de categorias
- `#42` Adicionar testes automatizados ao módulo de inventário




