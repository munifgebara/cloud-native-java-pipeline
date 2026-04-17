# Visão do Produto — Stella

## 1. Nome do produto

**Stella**

## 2. Propósito

O Stella é um sistema para controle de inventário pessoal por proprietário, permitindo cadastrar coisas, controlar suas instâncias individuais, registrar entradas, saídas, empréstimos e localizar cada item dentro de uma estrutura de locais hierárquicos.

## 3. Problema que o sistema resolve

Pessoas e famílias acumulam diversos bens ao longo do tempo, mas normalmente não possuem um sistema organizado para:

- saber o que possuem
- saber quantas unidades existem de um item
- registrar origem e valor de cada unidade
- registrar saídas e empréstimos
- localizar fisicamente cada instância
- separar corretamente o inventário de cada proprietário

O Stella resolve esse problema oferecendo um modelo estruturado de cadastro e movimentação de inventário.

## 4. Visão funcional resumida

O sistema deve permitir:

- cadastro e manutenção de proprietários
- segregação do inventário por proprietário
- cadastro de pessoas relacionadas ao inventário
- cadastro de coisas
- cadastro de instâncias de coisas
- entradas unitárias e múltiplas
- registro de saídas
- registro de empréstimos e devoluções
- controle de localização hierárquica
- atributos dinâmicos por tipo de coisa ou instância

## 5. Público usuário

### Administrador
Responsável por administrar a plataforma, incluindo:

- cadastro e manutenção de proprietários
- cadastro e manutenção de administradores

### Proprietário
Responsável por operar seu próprio inventário, incluindo:

- pessoas
- coisas
- instâncias
- entradas
- saídas
- empréstimos
- locais

## 6. Conceitos centrais do domínio

### Coisa
Representa o item base ou definição de algo cadastrável no inventário.

Exemplos:
- prato tipo A porcelana multicolorido
- livro
- computador
- bola

### Instância de Coisa
Representa a unidade física concreta de uma coisa.

Exemplo:
- prato azul comprado em uma data específica por um valor específico
- livro recebido de presente com valor estimado
- computador guardado em determinado local

### Pessoa
Representa alguém relacionado a operações de compra, venda, empréstimo ou outra movimentação.

### Local
Representa onde a instância está fisicamente armazenada, com suporte a hierarquia.

Exemplo:
- casa em Maringá
- quarto de casal
- closet
- prateleira superior

## 7. Premissas iniciais

- o inventário é isolado por proprietário
- uma coisa pode possuir várias instâncias
- cada instância pode possuir valor e data próprios
- presentes também possuem valor estimado de entrada
- locais são hierárquicos
- pessoas podem participar de entradas, saídas e empréstimos
- proprietários não cadastram outros proprietários
- o administrador do sistema mantém proprietários e administradores

## 8. Escopo inicial sugerido

### Dentro do escopo inicial
- proprietários
- pessoas
- coisas
- instâncias
- entradas
- saídas
- empréstimos
- locais hierárquicos
- segurança por perfil
- segregação por proprietário

### Fora do escopo inicial, por enquanto
- relatórios avançados
- importação em massa por arquivo
- anexos e fotos
- etiquetas e QR Code
- avaliações automáticas de valor
- integrações externas

## 9. Diretriz de modelagem

A modelagem deve separar claramente:

- **coisa** = definição do item
- **instância da coisa** = unidade individual controlável

Essa distinção é essencial para permitir que diferentes unidades do mesmo item tenham:

- cores diferentes
- datas diferentes de entrada
- valores diferentes
- locais diferentes
- históricos diferentes

## 10. Objetivo desta fase

Nesta fase, o objetivo é consolidar:

- entendimento funcional
- histórias de usuário
- regras de negócio
- entidades principais
- backlog inicial implementável
