# Entidades e Atributos — Stella

## Objetivo
Este documento descreve a proposta inicial das principais entidades do domínio do Stella.

---

## ENT-001 — Proprietario

Representa o dono do inventário.

### Atributos sugeridos
- `id`
- `nome`
- `ativo`
- `observacao`

### Observações
- toda informação operacional do inventário deve estar vinculada a um proprietário
- é o principal eixo de segregação de dados do sistema

---

## ENT-002 — Usuario

Representa o usuário autenticável do sistema.

### Atributos sugeridos
- `id`
- `nome`
- `email`
- `login`
- `ativo`

### Observações
- integra com o mecanismo de autenticação já existente
- pode estar ligado a uma pessoa
- pode ter papel de administrador ou proprietário, conforme estratégia adotada

---

## ENT-003 — Pessoa

Representa uma pessoa relacionada ao inventário.

### Exemplos
- de quem comprou
- para quem vendeu
- para quem emprestou
- quem devolveu
- quem presenteou

### Atributos sugeridos
- `id`
- `proprietarioId`
- `nome`
- `telefone`
- `email`
- `observacao`
- `usuarioId` opcional
- `ativo`

### Observações
- já existe base implementada no projeto para esta entidade
- deve respeitar escopo do proprietário

---

## ENT-004 — Coisa

Representa o item base do inventário.

### Exemplos
- prato tipo A porcelana multicolorido
- livro
- computador
- bola

### Atributos sugeridos
- `id`
- `proprietarioId`
- `nome`
- `descricao`
- `categoria`
- `tipoPadrao`
- `ativo`

### Observações
- representa o conceito base
- não corresponde necessariamente a uma unidade física única
- uma coisa pode possuir várias instâncias

---

## ENT-005 — InstanciaCoisa

Representa a unidade física individual de uma coisa.

### Exemplos
- prato azul específico
- livro específico recebido de presente
- computador específico guardado em um local

### Atributos sugeridos
- `id`
- `proprietarioId`
- `coisaId`
- `codigoInterno` opcional
- `situacao`
- `dataEntrada`
- `valorAquisicao`
- `valorEstimado`
- `localAtualId`
- `observacao`
- `ativo`

### Observações
- é uma das entidades centrais do domínio
- deve permitir rastreabilidade individual
- precisa suportar diferentes valores, datas e atributos por unidade

---

## ENT-006 — Local

Representa um local físico onde uma instância pode estar.

### Exemplos
- casa em Maringá
- quarto do casal
- closet
- porta tal
- prateleira tal

### Atributos sugeridos
- `id`
- `proprietarioId`
- `nome`
- `descricao`
- `localPaiId`
- `ativo`

### Observações
- deve permitir hierarquia
- precisa impedir ciclos
- pode ser usado para organizar o inventário fisicamente

---

## ENT-007 — Entrada

Representa a incorporação de uma instância ao inventário.

### Exemplos de tipo
- compra
- presente
- achado
- transferência de contexto futuro, se houver

### Atributos sugeridos
- `id`
- `proprietarioId`
- `instanciaCoisaId`
- `tipoEntrada`
- `dataEntrada`
- `valorPago`
- `valorEstimado`
- `pessoaOrigemId` opcional
- `observacao`

### Observações
- toda instância deve ter uma entrada inicial
- o valor pode ser pago ou estimado

---

## ENT-008 — Saida

Representa a saída definitiva de uma instância do inventário ativo.

### Exemplos de tipo
- venda
- doação
- perda
- estrago
- descarte

### Atributos sugeridos
- `id`
- `proprietarioId`
- `instanciaCoisaId`
- `tipoSaida`
- `dataSaida`
- `valorRecebido` opcional
- `pessoaDestinoId` opcional
- `observacao`

### Observações
- após saída definitiva, a instância não deve continuar disponível como ativa

---

## ENT-009 — Emprestimo

Representa o empréstimo de uma instância.

### Atributos sugeridos
- `id`
- `proprietarioId`
- `instanciaCoisaId`
- `pessoaDestinoId`
- `dataEmprestimo`
- `dataPrevistaDevolucao`
- `dataDevolucao`
- `status`
- `observacao`

### Observações
- empréstimo ativo impede novo empréstimo da mesma instância
- deve permitir registro de devolução

---

## ENT-010 — DefinicaoAtributo

Representa a definição de um atributo configurável de uma coisa.

### Exemplos
- cor
- tamanho
- autor
- memória
- processador
- esporte

### Atributos sugeridos
- `id`
- `proprietarioId`
- `coisaId`
- `nome`
- `tipo`
- `obrigatorio`
- `multiplo`
- `ordem`

### Observações
- permite configurar o “esquema flexível” por tipo de coisa
- pode evoluir futuramente para suportar modelos padrão

---

## ENT-011 — ValorAtributoInstancia

Representa o valor preenchido de um atributo em uma instância.

### Atributos sugeridos
- `id`
- `instanciaCoisaId`
- `definicaoAtributoId`
- `valorTexto`
- `valorNumero`
- `valorBooleano`

### Observações
- o tipo efetivo deve respeitar a definição do atributo
- uma mesma instância pode ter vários atributos preenchidos

---

## ENT-012 — ValorAtributoChaveValor

Representa estrutura flexível de chave-valor para complementar atributos.

### Atributos sugeridos
- `id`
- `instanciaCoisaId`
- `chave`
- `tipoValor`
- `valorTexto`
- `valorNumero`
- `valorBooleano`
- `ordem`

### Observações
- útil para acessórios e detalhes menos estruturados
- pode ser substituído ou incorporado por outra modelagem mais elegante depois

---

## Relacionamentos principais

### Proprietario
- possui muitas `Pessoa`
- possui muitas `Coisa`
- possui muitas `InstanciaCoisa`
- possui muitos `Local`
- possui muitas `Entrada`
- possui muitas `Saida`
- possui muitos `Emprestimo`

### Coisa
- possui muitas `InstanciaCoisa`
- possui muitas `DefinicaoAtributo`

### InstanciaCoisa
- pertence a uma `Coisa`
- pertence a um `Proprietario`
- pode ter um `Local` atual
- possui uma ou mais movimentações
- possui vários valores de atributos

### Local
- pertence a um `Proprietario`
- pode ter um `Local` pai
- pode conter várias instâncias

---

## Observações arquiteturais importantes

### 1. Separar coisa de instância
Essa separação é obrigatória no Stella.

### 2. Segregação por proprietário
Quase toda entidade de negócio deve carregar vínculo explícito ou inferível com `Proprietario`.

### 3. Histórico e auditoria
Entidades relevantes podem se beneficiar da base já existente de auditoria e Envers.

### 4. Exclusão lógica
Sempre que houver histórico associado, preferir inativação a remoção física.

### 5. Estados da instância
É recomendável definir enum ou máquina de estados para algo como:
- ATIVA
- EMPRESTADA
- VENDIDA
- DOADA
- PERDIDA
- DESCARTADA
