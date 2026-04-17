# Épicos — Stella

## Objetivo
Este documento organiza os grandes blocos funcionais do Stella em épicos, servindo como ponte entre a visão do produto, as histórias de usuário e o backlog implementável.

---

## EP-01 — Administração da plataforma

### Descrição
Este épico cobre as funcionalidades de administração global do sistema, restritas ao papel de administrador.

### Objetivos
- cadastrar proprietários
- manter proprietários
- cadastrar administradores
- manter administradores
- garantir controle de acesso por perfil

### Inclui
- gestão de proprietários
- gestão de administradores
- ativação e inativação de cadastros administrativos
- segurança por papel

### Não inclui
- operação do inventário de um proprietário
- movimentações de itens
- locais e atributos dinâmicos

---

## EP-02 — Pessoas

### Descrição
Este épico cobre o cadastro e manutenção de pessoas relacionadas ao inventário de cada proprietário.

### Objetivos
- registrar pessoas relacionadas a compras, vendas, empréstimos e outras interações
- manter vínculo opcional entre pessoa e usuário do sistema
- isolar dados por proprietário

### Inclui
- cadastro de pessoa
- edição e consulta de pessoa
- vínculo opcional com usuário
- uso da pessoa em movimentações

### Exemplos de uso
- de quem comprei um item
- para quem vendi
- para quem emprestei
- quem me presenteou

---

## EP-03 — Cadastro de coisas

### Descrição
Este épico cobre o cadastro do item base do inventário.

### Objetivos
- representar itens inventariáveis de maneira reutilizável
- permitir cadastro simples e manutenção do item base
- permitir uso de modelos padrão

### Inclui
- cadastro de coisa
- edição e inativação de coisa
- uso de coisas padrão
- categorização básica

### Exemplos
- prato tipo A porcelana multicolorido
- livro
- computador
- bola

---

## EP-04 — Controle de instâncias

### Descrição
Este épico cobre a gestão das unidades físicas concretas de cada coisa.

### Objetivos
- permitir controle individual por unidade
- permitir valores e datas próprios por instância
- permitir cadastro unitário e em lote

### Inclui
- criação de instância individual
- criação de múltiplas instâncias
- rastreabilidade individual
- controle de estado da instância

### Observação
Este é um dos épicos centrais do Stella, porque a essência do domínio depende da separação entre coisa e instância.

---

## EP-05 — Movimentações de inventário

### Descrição
Este épico cobre a entrada, saída, empréstimo e devolução de instâncias.

### Objetivos
- registrar como cada instância entrou no inventário
- registrar quando e por que saiu
- registrar empréstimos e devoluções
- manter consistência do estado da instância

### Inclui
- entrada
- saída
- empréstimo
- devolução
- validações de integridade de estado

### Exemplos
- compra
- presente
- venda
- doação
- perda
- descarte
- empréstimo temporário

---

## EP-06 — Locais e organização física

### Descrição
Este épico cobre a localização física das instâncias dentro de uma estrutura hierárquica de locais.

### Objetivos
- saber onde cada instância está
- modelar espaços reais de armazenamento
- permitir navegação por local e sublocal

### Inclui
- cadastro de locais
- hierarquia pai-filho
- vínculo da instância ao local atual
- movimentação entre locais

### Exemplos
- casa
- quarto
- closet
- gaveta
- prateleira

---

## EP-07 — Atributos dinâmicos

### Descrição
Este épico cobre a flexibilidade do domínio para permitir atributos específicos por tipo de coisa e por instância.

### Objetivos
- evitar modelagem rígida demais
- suportar diferenças entre domínios de itens
- permitir configuração por proprietário

### Inclui
- definição de atributos tipados
- preenchimento por instância
- suporte a texto, número e booleano
- estrutura complementar chave-valor

### Exemplos
- livro: autor, editora
- prato: cor, tamanho
- computador: memória, processador, acessórios
- bola: esporte, tamanho

---

## EP-08 — Segurança e segregação por proprietário

### Descrição
Este épico cobre a base de autorização e segregação de dados do Stella.

### Objetivos
- impedir acesso indevido entre proprietários
- aplicar restrições por papel
- proteger consultas e comandos da API

### Inclui
- escopo do proprietário
- controle por papel
- validações de acesso
- validações de integridade de contexto

### Observação
Este épico é transversal e afeta praticamente todos os demais.

---

## EP-09 — Catálogo inicial e experiência de uso inicial

### Descrição
Este épico cobre mecanismos para tornar o sistema mais fácil de começar a usar.

### Objetivos
- oferecer itens padrão
- reduzir trabalho manual do primeiro cadastro
- orientar a criação de estruturas comuns

### Inclui
- modelos padrão de coisas
- atributos pré-configurados
- futura evolução para assistentes de cadastro

---

## EP-10 — Consultas, histórico e auditoria

### Descrição
Este épico cobre a recuperação de informações consolidadas e históricas sobre o inventário.

### Objetivos
- consultar o inventário ativo
- consultar por local
- consultar histórico por instância
- aproveitar mecanismos já existentes de auditoria

### Inclui
- visão consolidada de inventário
- histórico da instância
- localização atual
- trilha de alterações relevantes

---

## Relação resumida entre os épicos

- `EP-01` sustenta a administração do sistema
- `EP-02` fornece os atores relacionados às movimentações
- `EP-03` define o item base
- `EP-04` define a unidade física real
- `EP-05` registra o ciclo de vida operacional da instância
- `EP-06` informa onde a instância está
- `EP-07` adiciona flexibilidade semântica ao cadastro
- `EP-08` garante isolamento e segurança
- `EP-09` melhora a experiência inicial
- `EP-10` consolida visão e rastreabilidade

