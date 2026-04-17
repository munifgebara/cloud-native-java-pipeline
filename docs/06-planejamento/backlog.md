# Backlog Inicial — Stella

## Objetivo
Organizar a evolução funcional inicial do Stella em uma sequência implementável.

---

# Fase 1 — Fundamentos do domínio

## ISSUE-001 — Criar entidade Proprietario
### Objetivo
Permitir a existência formal do contexto de proprietário no domínio.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-002 — Implementar CRUD de proprietários
### Objetivo
Permitir que administradores cadastrem, consultem, editem e inativem proprietários.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-003 — Implementar papel ADMIN e papel PROPRIETARIO
### Objetivo
Formalizar os papéis iniciais do sistema.

### Tipo
Backend / Segurança

### Prioridade
Alta

---

## ISSUE-004 — Garantir segregação de dados por proprietário
### Objetivo
Assegurar que todo dado operacional seja filtrado pelo proprietário correto.

### Tipo
Backend / Segurança / API

### Prioridade
Alta

---

## ISSUE-005 — Revisar cadastro de Pessoa para aderir ao contexto de proprietário
### Objetivo
Ajustar ou confirmar o módulo já existente de pessoas para o modelo multi-proprietário.

### Tipo
Backend / Frontend / Banco

### Prioridade
Alta

---

# Fase 2 — Coisas e instâncias

## ISSUE-006 — Criar entidade Coisa
### Objetivo
Representar o item base do inventário.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-007 — Implementar CRUD de coisas
### Objetivo
Permitir cadastro e manutenção de coisas.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-008 — Criar entidade InstanciaCoisa
### Objetivo
Representar a unidade individual rastreável de uma coisa.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-009 — Implementar cadastro de instância individual
### Objetivo
Permitir criação unitária de instâncias.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-010 — Implementar cadastro em lote de instâncias
### Objetivo
Permitir criação facilitada de múltiplas instâncias da mesma coisa.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

# Fase 3 — Movimentações

## ISSUE-011 — Criar entidade Entrada
### Objetivo
Registrar a incorporação de instâncias ao inventário.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-012 — Implementar registro de entrada
### Objetivo
Registrar entrada com data, origem e valor pago ou estimado.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-013 — Criar entidade Saida
### Objetivo
Registrar saída definitiva de instâncias.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-014 — Implementar registro de saída
### Objetivo
Permitir venda, doação, perda, estrago ou descarte.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-015 — Criar entidade Emprestimo
### Objetivo
Registrar empréstimos de instâncias para pessoas.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-016 — Implementar registro de empréstimo e devolução
### Objetivo
Permitir empréstimo ativo e devolução posterior.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-017 — Implementar regras de consistência de estado da instância
### Objetivo
Impedir transições inválidas como emprestar item já emprestado ou vender item já descartado.

### Tipo
Backend / Regras de negócio

### Prioridade
Alta

---

# Fase 4 — Locais

## ISSUE-018 — Criar entidade Local
### Objetivo
Permitir cadastrar locais físicos do inventário.

### Tipo
Backend / Banco

### Prioridade
Alta

---

## ISSUE-019 — Implementar CRUD de locais
### Objetivo
Permitir cadastro e manutenção de locais.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-020 — Implementar hierarquia de locais
### Objetivo
Permitir estruturar locais com pai e filho.

### Tipo
Backend / Frontend / Banco

### Prioridade
Alta

---

## ISSUE-021 — Vincular instância ao local atual
### Objetivo
Permitir saber onde cada instância está guardada.

### Tipo
Backend / Frontend / API

### Prioridade
Alta

---

## ISSUE-022 — Implementar movimentação de instância entre locais
### Objetivo
Permitir alterar local atual com consistência.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

# Fase 5 — Atributos dinâmicos

## ISSUE-023 — Criar entidade DefinicaoAtributo
### Objetivo
Permitir configuração de atributos tipados por coisa.

### Tipo
Backend / Banco

### Prioridade
Média

---

## ISSUE-024 — Criar entidade ValorAtributoInstancia
### Objetivo
Permitir armazenamento dos valores configurados por instância.

### Tipo
Backend / Banco

### Prioridade
Média

---

## ISSUE-025 — Implementar configuração de atributos dinâmicos
### Objetivo
Permitir que o proprietário configure atributos por coisa.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

## ISSUE-026 — Implementar preenchimento de atributos por instância
### Objetivo
Permitir que instâncias recebam valores específicos conforme a definição da coisa.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

## ISSUE-027 — Implementar suporte a chave-valor complementar
### Objetivo
Permitir estrutura mais flexível para acessórios e detalhes adicionais.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

# Fase 6 — Itens padrão e experiência inicial

## ISSUE-028 — Criar catálogo inicial de coisas padrão
### Objetivo
Disponibilizar itens padrão como Livro com atributos sugeridos.

### Tipo
Backend / Banco

### Prioridade
Média

---

## ISSUE-029 — Permitir criação de coisa a partir de modelo padrão
### Objetivo
Acelerar o cadastro inicial do usuário.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

# Fase 7 — Consultas e refinamentos

## ISSUE-030 — Criar consulta de inventário por proprietário
### Objetivo
Exibir visão consolidada do inventário ativo do proprietário.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

## ISSUE-031 — Criar consulta de inventário por local
### Objetivo
Permitir navegação por estrutura física e localização dos itens.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

## ISSUE-032 — Criar histórico da instância
### Objetivo
Exibir entrada, movimentações, empréstimos, devoluções e saída.

### Tipo
Backend / Frontend / API

### Prioridade
Média

---

## ISSUE-033 — Revisar auditoria com Envers para entidades principais
### Objetivo
Aproveitar infraestrutura existente para rastrear alterações relevantes.

### Tipo
Backend / Banco / Arquitetura

### Prioridade
Média

---

# Ordem prática recomendada

## Sprint ou etapa 1
- ISSUE-001
- ISSUE-002
- ISSUE-003
- ISSUE-004
- ISSUE-005

## Sprint ou etapa 2
- ISSUE-006
- ISSUE-007
- ISSUE-008
- ISSUE-009
- ISSUE-010

## Sprint ou etapa 3
- ISSUE-011
- ISSUE-012
- ISSUE-013
- ISSUE-014
- ISSUE-015
- ISSUE-016
- ISSUE-017

## Sprint ou etapa 4
- ISSUE-018
- ISSUE-019
- ISSUE-020
- ISSUE-021
- ISSUE-022

## Sprint ou etapa 5
- ISSUE-023
- ISSUE-024
- ISSUE-025
- ISSUE-026
- ISSUE-027

---

# Observações finais

## 1. Separar documento de execução
Este backlog é o mapa funcional inicial.  
A execução detalhada deve ser feita em GitHub Issues ou Jira.

## 2. Relacionar backlog com histórias
Sempre que possível, cada issue implementável deve apontar para uma ou mais histórias de usuário.

## 3. Evitar começar pelos atributos dinâmicos
Apesar de importantes, os atributos dinâmicos trazem complexidade de modelagem e interface.  
É melhor consolidar primeiro:
- proprietário
- pessoa
- coisa
- instância
- movimentações
- local

## 4. Proteger o núcleo do domínio
As regras de segregação por proprietário e consistência de estado precisam ser tratadas desde o início.

