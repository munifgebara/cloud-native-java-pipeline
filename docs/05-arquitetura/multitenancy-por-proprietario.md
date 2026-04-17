# Multitenancy por Proprietário — Stella

## Objetivo

Descrever a estratégia de segregação de dados do Stella por proprietário, considerando:

- o modelo funcional do domínio
- o atributo `oi` já existente na entidade base
- a necessidade de isolamento entre contextos
- a possibilidade de hierarquia organizacional futura

---

# 1. Contexto

O Stella é um sistema de inventário em que cada proprietário mantém seu próprio conjunto de dados.

Na prática, isso significa que:

- um proprietário não pode acessar dados de outro
- consultas devem ser filtradas pelo contexto correto
- comandos devem validar o escopo do recurso manipulado
- o modelo precisa suportar segurança, consistência e evolução futura

O projeto já possui uma base comum de persistência com o atributo `oi` na superclasse `Entidade`, implementado como `String`.

Essa decisão é relevante porque permite usar `oi` como identificador organizacional textual e potencialmente hierárquico.

---

# 2. Conceitos envolvidos

## Proprietário

É o conceito funcional do domínio.  
Representa o dono do inventário.

Exemplos:

- Munif
- Vicente
- outro membro da família
- outro usuário dono do próprio inventário

O proprietário é uma entidade de negócio.

---

## OI

`oi` é um atributo técnico/organizacional presente na entidade base.

Ele não é, por si só, o conceito funcional de proprietário.  
Seu papel é servir como identificador organizacional transversal para escopo, segmentação, filtragem e eventual hierarquia.

Como é `String`, ele pode representar estruturas como:

- `001`
- `001.002`
- `familia.munif`
- `familia.munif.vicente`
- `stella.casa.maringa`

---

# 3. Decisão arquitetural proposta

## Decisão

O Stella deve tratar **proprietário** como conceito de domínio e **oi** como conceito arquitetural de escopo organizacional.

Em outras palavras:

- `proprietario` representa o dono funcional dos dados
- `oi` representa o contexto organizacional/técnico no qual a entidade existe

Esses conceitos podem se relacionar, mas não devem ser confundidos.

---

# 4. Papel de cada elemento

## 4.1. Papel do `proprietario`

O `proprietario` deve ser usado para:

- expressar pertença de negócio
- modelar regras funcionais
- relacionar entidades do inventário
- deixar o domínio compreensível
- orientar a API e a interface

Exemplos de entidades claramente pertencentes a um proprietário:

- `Pessoa`
- `Coisa`
- `InstanciaCoisa`
- `Local`
- `Entrada`
- `Saida`
- `Emprestimo`

---

## 4.2. Papel do `oi`

O `oi` deve ser usado para:

- escopo técnico transversal
- apoio à segregação organizacional
- eventual filtragem global
- suporte a hierarquia organizacional
- futura generalização de contexto além de um proprietário simples

O `oi` é especialmente útil quando o sistema quiser evoluir para cenários como:

- famílias
- grupos
- estruturas organizacionais
- unidades dentro de uma estrutura maior
- subcontextos com herança ou prefixo

---

# 5. Por que não usar apenas `oi`

Usar apenas `oi` para tudo teria algumas vantagens:

- menos campos explícitos nas entidades
- filtragem uniforme
- estrutura pronta para hierarquia

Mas traria problemas importantes:

- o domínio ficaria menos claro
- regras de negócio ficariam implícitas demais
- relacionamentos funcionais perderiam legibilidade
- a modelagem ficaria excessivamente técnica
- APIs e telas passariam a depender de convenções internas

Exemplo de problema:

Se `InstanciaCoisa` não tiver vínculo funcional claro com `Proprietario`, a pergunta “de quem é esse item?” deixa de ser respondida diretamente pelo domínio e passa a depender de interpretação de `oi`.

Isso empobrece o modelo de negócio.

---

# 6. Por que não ignorar `oi`

Também não é ideal ignorar o `oi`, porque ele já existe na base comum e oferece vantagens reais:

- uniformidade na camada técnica
- possibilidade de filtros transversais
- suporte futuro a hierarquia
- melhor preparação para evolução de escopo
- possível integração com regras de segurança mais genéricas

Portanto, o caminho mais equilibrado é:

- não substituir o domínio por `oi`
- não tratar `oi` como mero campo sem uso
- definir papéis claros para ambos

---

# 7. Estratégia recomendada para o Stella

## Regra geral

Para entidades operacionais relevantes do domínio, o Stella pode manter:

- vínculo funcional com `Proprietario`
- `oi` herdado da superclasse como contexto organizacional

## Interpretação prática

- `proprietarioId` responde: **de quem é isso no negócio**
- `oi` responde: **em que contexto organizacional/técnico isso existe**

---

# 8. Entidades em que a redundância é aceitável

A redundância é justificável quando ela reduz complexidade, melhora legibilidade e reforça segurança.

Isso faz sentido principalmente em:

- `Pessoa`
- `Coisa`
- `InstanciaCoisa`
- `Local`
- `Entrada`
- `Saida`
- `Emprestimo`

Nessas entidades:

- `proprietarioId` deixa o domínio explícito
- `oi` mantém o alinhamento com a base arquitetural

---

# 9. Vantagens da redundância controlada

## 9.1. Clareza do domínio

Quem lê a entidade entende rapidamente a quem ela pertence.

## 9.2. Segurança

Facilita validações explícitas de escopo.

## 9.3. Consultas

Permite filtros mais simples e previsíveis.

## 9.4. Evolução

Permite que o papel de `oi` cresça no futuro sem destruir a semântica do domínio atual.

## 9.5. Integração com a base comum

Mantém aderência à infraestrutura já implementada.

---

# 10. Risco da redundância

A redundância também traz riscos:

- inconsistência entre `oi` e `proprietarioId`
- duplicação desnecessária de regra
- aumento de responsabilidade na aplicação
- maior cuidado em persistência e atualização

Por isso, a redundância só é válida se houver uma regra clara de consistência.

---

# 11. Regra de consistência proposta

## Regra principal

Sempre que uma entidade de domínio pertencer a um proprietário, seu `oi` deve ser compatível com o contexto organizacional daquele proprietário.

Isso implica que:

- a aplicação deve definir como o `oi` do proprietário é formado
- as entidades derivadas devem receber `oi` coerente
- não pode haver recurso com `proprietarioId` de um contexto e `oi` de outro

---

# 12. Estratégias possíveis de preenchimento do `oi`

## Estratégia A — OI igual ao identificador lógico do proprietário

Exemplo:

- Proprietário Munif → `oi = proprietario.munif`
- Pessoa do Munif → `oi = proprietario.munif`
- Coisa do Munif → `oi = proprietario.munif`

### Vantagens
- simples
- fácil de entender
- fácil de filtrar

### Desvantagens
- pouca expressividade hierárquica interna

---

## Estratégia B — OI hierárquico por contexto

Exemplo:

- Proprietário Munif → `oi = familia.munif`
- Pessoa do Munif → `oi = familia.munif`
- Local do Munif → `oi = familia.munif`
- contexto futuro de suborganização → `oi = familia.munif.casa1`

### Vantagens
- mais preparada para evolução
- melhor suporte a prefixos e agrupamentos

### Desvantagens
- exige convenção melhor definida

---

## Estratégia C — OI técnico desacoplado do nome de negócio

Exemplo:

- Proprietário Munif → `oi = 001.0001`
- dados subordinados → `oi = 001.0001`

### Vantagens
- estabilidade
- independência de nome amigável

### Desvantagens
- menos legível
- exige lookup adicional para interpretação

---

# 13. Recomendação atual

A melhor escolha inicial para o Stella parece ser a **Estratégia B**, com `oi` textual e compatível com hierarquia.

Motivos:

- já existe implementação em `String`
- você explicitou intenção de usar hierarquia
- o domínio pode evoluir além de um simples dono isolado
- continua legível se bem padronizado

---

# 14. Convenção recomendada para o `oi`

## Regras sugeridas

- usar apenas minúsculas
- usar separador fixo, preferencialmente `.`
- evitar espaços
- evitar acentos
- evitar dependência de nome exibido ao usuário
- definir tamanho máximo coerente com a coluna
- manter estabilidade mesmo se o nome visual mudar

## Exemplos válidos

- `familia.munif`
- `familia.munif.vicente`
- `grupo.estoque.casa1`

## Exemplos a evitar

- `Casa do Munif`
- `Munif/Filho`
- `Inventário Maringá`
- valores dependentes de texto instável de tela

---

# 15. Impacto no modelo de entidades

## Entidade base

A entidade base continua fornecendo:

- `id`
- `oi`
- atributos comuns de auditoria e controle

## Entidades de domínio

As entidades de domínio continuam expressando relações funcionais explícitas, como:

- `proprietario`
- `pessoa`
- `coisa`
- `instancia`
- `local`

Ou seja, a base comum não substitui o modelo do domínio.

---

# 16. Impacto nas consultas

As consultas podem usar:

- `proprietarioId` para lógica de negócio explícita
- `oi` para filtros transversais ou organizacionais

## Exemplos

### Consulta de negócio
“listar todas as coisas do proprietário X”

Melhor guiada por:
- `proprietarioId`

### Consulta organizacional
“listar todos os registros do contexto cujo `oi` começa com `familia.munif`”

Melhor guiada por:
- `oi`

---

# 17. Impacto na segurança

A segurança deve validar, no mínimo:

- perfil do usuário
- proprietário associado ao contexto
- compatibilidade do recurso com o contexto
- coerência entre `proprietarioId` e `oi`, quando ambos existirem

O ideal é que o sistema nunca dependa apenas de convenção informal.

---

# 18. Impacto na API

A API pública deve continuar centrada no domínio, e não expor `oi` como conceito principal de negócio, salvo necessidade específica.

Ou seja:

- o usuário fala em proprietário
- a API fala em proprietário
- a modelagem funcional fala em proprietário
- `oi` permanece mais interno/arquitetural, salvo casos em que faça sentido expô-lo

Isso reduz acoplamento da API com detalhes técnicos da persistência.

---

# 19. Decisão resumida

## Decisão arquitetural adotada

O Stella usa `oi` como identificador organizacional textual e potencialmente hierárquico, presente na entidade base.

O Stella também mantém vínculos explícitos de domínio com `Proprietario` nas entidades operacionais relevantes.

Essa combinação é uma estratégia de **redundância controlada**, aceita porque traz:

- clareza funcional
- flexibilidade arquitetural
- apoio à segurança
- melhor capacidade de evolução

---

# 20. Regras práticas para implementação

## RP-01
Entidades de domínio do inventário devem continuar modelando o vínculo funcional com `Proprietario`.

## RP-02
O valor de `oi` deve ser coerente com o contexto do proprietário.

## RP-03
Não pode existir entidade com `proprietarioId` e `oi` apontando para contextos incompatíveis.

## RP-04
A aplicação deve centralizar a geração e validação do `oi`.

## RP-05
Consultas e segurança podem usar `oi`, mas o domínio não deve depender exclusivamente dele.

---

# 21. Decisões futuras em aberto

Os seguintes pontos ainda devem ser definidos em detalhe:

- formato final oficial do `oi`
- regra de unicidade
- se o `oi` do proprietário será derivado de slug, código ou estrutura própria
- se haverá busca por prefixo hierárquico
- se haverá índice específico para `oi`
- quais entidades realmente precisarão de redundância explícita com `proprietarioId`

---

# 22. Conclusão

No Stella, `oi` e `proprietario` não são concorrentes; eles cumprem papéis diferentes.

- `proprietario` organiza o **domínio**
- `oi` organiza o **escopo arquitetural**

A decisão mais robusta, neste momento do projeto, é manter ambos com responsabilidade bem definida e evitar tanto:

- a eliminação prematura da redundância útil
- quanto a duplicação sem regra de consistência

