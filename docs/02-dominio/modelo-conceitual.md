# Modelo Conceitual — Stella

## Objetivo

Descrever o domínio principal do Stella em nível conceitual, com foco nos conceitos centrais e na forma como eles se relacionam.

---

# 1. Ideia central do sistema

O Stella é um sistema de inventário orientado a proprietários.  
Cada proprietário possui seu próprio conjunto de dados e opera seu próprio inventário de forma isolada.

O sistema existe para responder perguntas como:

- o que eu tenho?
- quantas unidades eu tenho?
- onde cada unidade está?
- quando essa unidade entrou?
- quanto ela custou ou quanto vale aproximadamente?
- para quem eu emprestei?
- para quem vendi?
- o item ainda está comigo ou já saiu?

---

# 2. Conceito mais importante do domínio

O ponto central da modelagem é a separação entre:

## Coisa

É a definição do item inventariável.

Exemplos:

- prato tipo A porcelana multicolorido
- livro
- computador
- bola

## Instância da Coisa

É a unidade física concreta daquele item.

Exemplos:

- prato azul comprado em uma data específica
- prato verde recebido como presente
- computador com determinada memória guardado em certo local
- um livro específico emprestado a alguém

Sem essa separação, o sistema perde capacidade de representar corretamente:

- múltiplas unidades
- valores diferentes
- datas diferentes
- locais diferentes
- estados diferentes
- históricos diferentes

---

# 3. Proprietário como eixo de segregação

Todo inventário pertence a um proprietário.

O proprietário é o contexto principal do negócio.  
Praticamente toda entidade operacional do sistema deve estar direta ou indiretamente vinculada a ele.

Isso significa que:

- pessoas pertencem a um proprietário
- coisas pertencem a um proprietário
- instâncias pertencem a um proprietário
- locais pertencem ao proprietário
- movimentações pertencem ao contexto do proprietário

Essa regra é essencial para segurança e consistência do sistema.

---

# 4. Pessoas como participantes das movimentações

No Stella, pessoa não é apenas um cadastro genérico.  
Pessoa representa alguém que participa de eventos do inventário.

Exemplos:

- de quem o proprietário comprou
- quem presenteou
- para quem vendeu
- para quem emprestou
- quem devolveu um item

Em alguns casos, uma pessoa também pode estar ligada a um usuário do sistema.

---

# 5. Coisa como tipo base inventariável

A coisa representa o item base que pode ser controlado no inventário.

Ela funciona como um “modelo concreto do proprietário”, não necessariamente como uma unidade física.

Exemplo:

- Coisa: “Prato tipo A porcelana multicolorido”

Essa coisa pode originar várias instâncias:

- prato verde 1
- prato verde 2
- prato azul 1
- prato vermelho 1
- etc.

A coisa concentra o que é comum ou estrutural.  
A instância concentra o que é individual.

---

# 6. Instância como unidade rastreável

A instância é a unidade física real do inventário.

É nela que recaem perguntas como:

- entrou quando?
- custou quanto?
- vale quanto?
- está onde?
- está emprestada?
- já saiu do inventário?
- qual é o seu histórico?

A instância é o centro operacional do sistema.

---

# 7. Movimentações como ciclo de vida da instância

Uma instância não é estática.  
Ela passa por eventos ao longo do tempo.

Os principais eventos iniciais do Stella são:

## Entrada

Marca a incorporação da instância ao inventário.

Exemplos:

- compra
- presente
- achado

## Saída

Marca a retirada definitiva da instância do inventário ativo.

Exemplos:

- venda
- doação
- perda
- estrago
- descarte

## Empréstimo

Marca a saída temporária da instância para outra pessoa, com possibilidade de devolução posterior.

## Devolução

Marca o retorno da instância ao inventário disponível após empréstimo.

Esses eventos compõem o histórico da instância e definem seu estado atual.

---

# 8. Estado da instância

Além do histórico, a instância precisa ter um estado atual coerente.

Estados iniciais sugeridos:

- ATIVA
- EMPRESTADA
- VENDIDA
- DOADA
- PERDIDA
- DESCARTADA

As regras de negócio devem controlar as transições possíveis entre esses estados.

Exemplos:

- uma instância emprestada não pode ser emprestada novamente
- uma instância vendida não pode continuar ativa
- uma instância descartada não pode receber novo empréstimo

---

# 9. Local como representação do espaço físico

O Stella precisa dizer onde a instância está fisicamente.

Para isso, existe o conceito de local.

Exemplos:

- casa em Maringá
- quarto do casal
- closet
- porta tal
- prateleira tal

O local deve suportar hierarquia, porque o espaço real costuma ser organizado dessa forma.

Exemplo de árvore:

- Casa em Maringá
  - Quarto do casal
    - Closet
      - Prateleira superior

A instância aponta para um local atual.

---

# 10. Atributos dinâmicos como flexibilidade do domínio

Nem toda coisa possui as mesmas características.

Exemplos:

- um prato pode ter cor e tamanho
- uma bola pode ter esporte e tamanho
- um livro pode ter autor
- um computador pode ter memória, processador e acessórios

Por isso, o Stella precisa suportar atributos dinâmicos.

Esses atributos podem ser:

- texto
- número
- booleano

E podem ainda ser complementados por estruturas chave-valor quando o domínio exigir mais flexibilidade.

---

# 11. Coisas padrão como aceleradores de cadastro

O sistema pode oferecer modelos padrão de coisas.

Exemplo:

- Livro já vir com atributos como autor, editora e gênero
- Computador já vir com memória e processador
- Prato já vir com cor e tamanho

Esses modelos existem para acelerar o uso inicial do sistema, mas continuam subordinados ao contexto do proprietário.

---

# 12. Papéis do sistema

## Administrador

Responsável pela administração da plataforma.

Pode:

- cadastrar proprietários
- manter proprietários
- cadastrar administradores
- manter administradores

## Proprietário

Responsável pela operação do inventário no próprio contexto.

Pode:

- cadastrar pessoas
- cadastrar coisas
- cadastrar instâncias
- registrar entradas
- registrar saídas
- registrar empréstimos
- registrar devoluções
- cadastrar locais
- configurar atributos

Não pode:

- cadastrar proprietários
- administrar a plataforma global

---

# 13. Limite entre cadastro e operação

No domínio do Stella, é útil distinguir:

## Cadastro estrutural

Define a base do inventário:

- proprietários
- pessoas
- coisas
- locais
- definições de atributos

## Operação do inventário

Altera a vida das unidades concretas:

- criação de instâncias
- entradas
- saídas
- empréstimos
- devoluções
- mudança de local

Essa separação ajuda na organização da API, da interface e das regras de negócio.

---

# 14. Histórico e auditoria

Como o inventário envolve mudança ao longo do tempo, o sistema se beneficia muito de histórico e auditoria.

Dois níveis de rastreabilidade são importantes:

## Histórico de negócio

Eventos da vida da instância:

- entrada
- empréstimo
- devolução
- saída
- mudança relevante de local

## Auditoria técnica

Alterações de cadastro:

- quem alterou
- quando alterou
- o que mudou

O projeto já possui base de auditoria que pode ser aproveitada.

---

# 15. Resumo conceitual

O modelo conceitual do Stella pode ser resumido assim:

- um `Administrador` governa a plataforma
- um `Proprietário` governa seu próprio inventário
- o proprietário cadastra `Pessoas`, `Coisas` e `Locais`
- cada `Coisa` pode ter várias `Instancias`
- cada `Instancia` possui estado, valor, data, atributos e local atual
- cada instância passa por `Entradas`, `Saidas` e `Emprestimos`
- `Locais` organizam o espaço físico em hierarquia
- `Atributos dinâmicos` permitem flexibilidade entre diferentes tipos de coisa
- toda a operação deve respeitar o contexto do proprietário

