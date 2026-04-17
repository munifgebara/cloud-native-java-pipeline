# Regras de Negócio — Stella

## RN-01 — Segregação obrigatória por proprietário
Todo dado operacional do inventário deve pertencer a exatamente um proprietário.

## RN-02 — Isolamento de acesso
Um proprietário não pode visualizar, editar ou movimentar dados pertencentes a outro proprietário.

## RN-03 — Administração de proprietários
Somente administradores do sistema podem cadastrar e manter proprietários.

## RN-04 — Administração de administradores
Somente administradores do sistema podem cadastrar e manter outros administradores.

## RN-05 — Escopo do proprietário
O proprietário pode operar todos os recursos do seu contexto, exceto cadastro e manutenção de proprietários e administradores.

## RN-06 — Pessoa vinculada ao contexto do proprietário
Toda pessoa usada em compras, vendas, empréstimos ou outras movimentações deve pertencer ao contexto do proprietário.

## RN-07 — Pessoa pode estar ligada a usuário
Uma pessoa pode possuir vínculo com um usuário do sistema, conforme necessidade de autenticação e rastreabilidade.

## RN-08 — Coisa é diferente de instância
O sistema deve separar o conceito de coisa do conceito de instância da coisa.

## RN-09 — Uma coisa pode possuir várias instâncias
Uma coisa pode representar um item base com uma ou várias unidades físicas individualizáveis.

## RN-10 — Cada instância possui identidade própria
Cada instância deve poder possuir dados próprios, como:
- data de entrada
- valor de aquisição
- valor estimado
- local atual
- atributos específicos
- situação atual

## RN-11 — Entrada pode ter valor pago ou estimado
Toda entrada deve permitir o registro de valor financeiro.  
Quando não houver compra, o valor pode ser estimado.

## RN-12 — Presentes também entram no inventário
Instâncias recebidas como presente devem ser registradas como entrada, com valor estimado quando não houver valor pago.

## RN-13 — Deve existir facilitador para entradas múltiplas
O sistema deve permitir registrar múltiplas instâncias de uma mesma coisa de forma facilitada.

## RN-14 — Entrada em lote não elimina individualidade
Mesmo quando cadastradas em lote, as instâncias devem continuar sendo individualmente rastreáveis.

## RN-15 — Coisas podem ter atributos configuráveis
Cada coisa pode possuir atributos configuráveis de diferentes tipos.

## RN-16 — Tipos básicos de atributos
Os atributos devem suportar, no mínimo:
- texto
- número
- booleano

## RN-17 — Suporte a estruturas chave-valor
O sistema deve suportar atributos compostos ou multivalorados em estrutura de chave-valor, conforme necessidade do domínio.

## RN-18 — Coisas padrão
O sistema pode oferecer algumas coisas padrão com atributos pré-configurados, como por exemplo livros.

## RN-19 — Toda instância deve possuir situação controlada
Toda instância deve possuir uma situação consistente, como por exemplo:
- ativa em inventário
- emprestada
- vendida
- doada
- perdida
- descartada

## RN-20 — Instância emprestada não pode ser emprestada novamente
Uma instância em empréstimo ativo não pode receber novo empréstimo até que haja devolução.

## RN-21 — Instância com saída definitiva não pode continuar disponível
Uma instância vendida, doada, perdida ou descartada não pode continuar disponível como se estivesse no inventário ativo.

## RN-22 — Saída deve registrar motivo
Toda saída deve possuir um motivo ou tipo de saída.

Exemplos:
- venda
- doação
- perda
- estrago
- descarte

## RN-23 — Empréstimo deve registrar destinatário
Todo empréstimo deve registrar a pessoa para quem a instância foi emprestada.

## RN-24 — Empréstimo pode registrar devolução
O sistema deve permitir registrar a devolução de uma instância emprestada.

## RN-25 — Toda instância pode possuir local atual
Cada instância deve possuir um local atual dentro do inventário do proprietário.

## RN-26 — Locais pertencem ao proprietário
Todo local deve pertencer ao contexto do proprietário.

## RN-27 — Locais podem ser hierárquicos
O sistema deve suportar locais com relação pai-filho.

Exemplos:
- Casa
  - Quarto
    - Closet
      - Prateleira

## RN-28 — A hierarquia de locais não pode ter ciclos
Um local não pode ser ancestral de si mesmo, direta ou indiretamente.

## RN-29 — Movimentação de local deve preservar consistência
Ao alterar o local de uma instância, o sistema deve manter o local atual consistente.

## RN-30 — Exclusão física deve ser restrita
Entidades com histórico relevante não devem ser removidas fisicamente sem regra explícita.  
Preferencialmente deve-se usar inativação lógica quando aplicável.

## RN-31 — Auditoria é desejável
Alterações relevantes de cadastro e movimentação devem ser auditáveis, aproveitando a infraestrutura já existente no projeto quando possível.

## RN-32 — Toda API deve respeitar contexto e perfil
As APIs devem validar:
- papel do usuário
- escopo do proprietário
- consistência do estado da instância
