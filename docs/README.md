# Documentação do Stella

Esta pasta centraliza a documentação funcional e técnica do Stella.

## Estrutura inicial

### Visão geral
- [Visão do produto](00-visao-geral/visao-produto.md)

### Requisitos
- [Regras de negócio](01-requisitos/regras-de-negocio.md)
- [Histórias de usuário](01-requisitos/historias-de-usuario.md)

### Domínio
- [Entidades e atributos](02-dominio/entidades-e-atributos.md)

### Planejamento
- [Backlog inicial](06-planejamento/backlog.md)

## Objetivo desta documentação

Organizar de forma versionada e rastreável:

- visão funcional do sistema
- regras de negócio
- histórias de usuário
- modelo de domínio
- backlog inicial de evolução

## Convenções sugeridas

### Prefixos
- `EP` = Épico
- `HU` = História de Usuário
- `RN` = Regra de Negócio
- `ENT` = Entidade
- `ISSUE` = Item implementável

### Papéis do sistema
- `ADMIN` = administra a plataforma, proprietários e administradores
- `PROPRIETARIO` = opera o próprio inventário

## Observação importante

O Stella é um sistema multi-tenant por contexto de proprietário.  
Toda entidade de negócio operacional deve respeitar a segregação por proprietário.
