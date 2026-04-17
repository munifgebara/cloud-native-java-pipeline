# Relacionamentos — Stella

## Objetivo

Descrever os principais relacionamentos entre as entidades do domínio do Stella.

---

# 1. Visão resumida

O domínio do Stella gira em torno de um proprietário, que mantém seu inventário separado dos demais.

Em torno dele orbitam:

- pessoas
- coisas
- instâncias
- locais
- movimentações
- atributos dinâmicos

---

# 2. Relacionamentos principais por entidade

## Proprietario

### Relacionamentos

- `Proprietario 1:N Pessoa`
- `Proprietario 1:N Coisa`
- `Proprietario 1:N InstanciaCoisa`
- `Proprietario 1:N Local`
- `Proprietario 1:N Entrada`
- `Proprietario 1:N Saida`
- `Proprietario 1:N Emprestimo`

### Observações

- é o principal delimitador de escopo
- todo relacionamento operacional deve respeitar esse contexto

---

## Usuario

### Relacionamentos

- `Usuario 0:1 Pessoa`

### Observações

- o vínculo com pessoa pode ser opcional
- papéis e autenticação ficam associados ao usuário
- a forma exata de vínculo com proprietário dependerá da estratégia de segurança adotada no projeto

---

## Pessoa

### Relacionamentos

- `Pessoa N:1 Proprietario`
- `Pessoa 0:1 Usuario`
- `Pessoa 1:N Entrada` como origem, quando aplicável
- `Pessoa 1:N Saida` como destino, quando aplicável
- `Pessoa 1:N Emprestimo` como destinatário

### Observações

- pessoa sempre pertence ao contexto de um proprietário
- uma pessoa pode participar de várias movimentações

---

## Coisa

### Relacionamentos

- `Coisa N:1 Proprietario`
- `Coisa 1:N InstanciaCoisa`
- `Coisa 1:N DefinicaoAtributo`

### Observações

- representa o item base
- não representa diretamente a unidade física
- concentra a definição estrutural do item

---

## InstanciaCoisa

### Relacionamentos

- `InstanciaCoisa N:1 Proprietario`
- `InstanciaCoisa N:1 Coisa`
- `InstanciaCoisa N:1 Local` como local atual, opcionalmente
- `InstanciaCoisa 1:N ValorAtributoInstancia`
- `InstanciaCoisa 1:N ValorAtributoChaveValor`
- `InstanciaCoisa 1:N Entrada`
- `InstanciaCoisa 1:N Saida`
- `InstanciaCoisa 1:N Emprestimo`

### Observações

- é a unidade física controlável
- concentra o estado operacional real do inventário
- é a entidade que mais exige integridade de regra de negócio

---

## Local

### Relacionamentos

- `Local N:1 Proprietario`
- `Local N:1 Local` como pai, opcional
- `Local 1:N Local` como filhos
- `Local 1:N InstanciaCoisa`

### Observações

- suporta hierarquia pai-filho
- deve impedir ciclos
- uma instância aponta para um local atual

---

## Entrada

### Relacionamentos

- `Entrada N:1 Proprietario`
- `Entrada N:1 InstanciaCoisa`
- `Entrada N:1 Pessoa` como origem, opcional

### Observações

- idealmente cada instância possui ao menos uma entrada inicial
- entradas em lote podem gerar múltiplas instâncias e múltiplos registros associados

---

## Saida

### Relacionamentos

- `Saida N:1 Proprietario`
- `Saida N:1 InstanciaCoisa`
- `Saida N:1 Pessoa` como destino, opcional

### Observações

- saída tende a representar evento definitivo
- precisa afetar o estado da instância

---

## Emprestimo

### Relacionamentos

- `Emprestimo N:1 Proprietario`
- `Emprestimo N:1 InstanciaCoisa`
- `Emprestimo N:1 Pessoa` como destinatário

### Observações

- empréstimo pode ter devolução registrada no próprio agregado
- apenas empréstimos ativos devem impedir novo empréstimo da mesma instância

---

## DefinicaoAtributo

### Relacionamentos

- `DefinicaoAtributo N:1 Proprietario`
- `DefinicaoAtributo N:1 Coisa`
- `DefinicaoAtributo 1:N ValorAtributoInstancia`

### Observações

- define o esquema flexível de uma coisa
- cada coisa pode ter várias definições de atributos

---

## ValorAtributoInstancia

### Relacionamentos

- `ValorAtributoInstancia N:1 InstanciaCoisa`
- `ValorAtributoInstancia N:1 DefinicaoAtributo`

### Observações

- armazena o valor concreto de um atributo para uma instância
- deve respeitar o tipo definido em `DefinicaoAtributo`

---

## ValorAtributoChaveValor

### Relacionamentos

- `ValorAtributoChaveValor N:1 InstanciaCoisa`

### Observações

- estrutura complementar mais flexível
- útil para acessórios e dados menos previsíveis

---

# 3. Relacionamentos em formato textual resumido

## Núcleo de escopo

- um `Proprietario` possui muitas `Pessoa`
- um `Proprietario` possui muitas `Coisa`
- um `Proprietario` possui muitas `InstanciaCoisa`
- um `Proprietario` possui muitos `Local`
- um `Proprietario` possui muitas movimentações

## Núcleo do inventário

- uma `Coisa` possui muitas `InstanciaCoisa`
- uma `InstanciaCoisa` pertence a uma `Coisa`
- uma `InstanciaCoisa` pode estar em um `Local`

## Núcleo de movimentação

- uma `InstanciaCoisa` possui entradas
- uma `InstanciaCoisa` possui saídas
- uma `InstanciaCoisa` possui empréstimos
- uma `Pessoa` pode participar dessas movimentações

## Núcleo de flexibilidade

- uma `Coisa` possui várias `DefinicaoAtributo`
- uma `InstanciaCoisa` possui vários `ValorAtributoInstancia`
- uma `InstanciaCoisa` pode possuir vários registros complementares de chave-valor

---

# 4. Regras de consistência entre relacionamentos

## RC-01 — Contexto único por proprietário

Uma entidade operacional não pode se relacionar com outra de proprietário diferente.

### Exemplos

- uma instância de um proprietário não pode apontar para local de outro proprietário
- uma saída de um proprietário não pode usar instância de outro proprietário
- uma pessoa usada em empréstimo deve estar no mesmo contexto do proprietário da instância

---

## RC-02 — Integridade coisa-instância

Toda instância deve referenciar exatamente uma coisa.

---

## RC-03 — Integridade local-instância

Se uma instância possuir local atual, esse local deve pertencer ao mesmo proprietário da instância.

---

## RC-04 — Integridade atributo-instância

Um valor de atributo de instância só pode existir se houver definição de atributo compatível.

---

## RC-05 — Integridade de hierarquia de locais

A árvore de locais não pode formar ciclos.

---

## RC-06 — Integridade de movimentação

Saídas e empréstimos só podem ocorrer sobre instâncias em estado compatível.

---

# 5. Diagrama textual simplificado

```text
Proprietario
 ├── Pessoa
 ├── Coisa
 │    ├── DefinicaoAtributo
 │    └── InstanciaCoisa
 │         ├── ValorAtributoInstancia
 │         ├── ValorAtributoChaveValor
 │         ├── Entrada
 │         ├── Saida
 │         └── Emprestimo
 └── Local
      └── Local (filhos)