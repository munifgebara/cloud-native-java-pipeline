# Histórias de Usuário — Stella

## Épico EP-01 — Administração da plataforma

### HU-001 — Cadastrar proprietário
Como administrador do sistema,  
quero cadastrar proprietários,  
para que cada um tenha seu inventário separado.

#### Critérios de aceite
- o administrador pode criar um proprietário
- o proprietário fica disponível para uso no sistema
- os dados dos proprietários ficam segregados

---

### HU-002 — Manter proprietário
Como administrador do sistema,  
quero editar e inativar proprietários,  
para manter os dados cadastrais corretos.

#### Critérios de aceite
- o administrador pode listar proprietários
- o administrador pode editar dados
- o administrador pode inativar um proprietário

---

### HU-003 — Cadastrar administrador
Como administrador do sistema,  
quero cadastrar outros administradores,  
para delegar a administração da plataforma.

#### Critérios de aceite
- administradores podem criar outros administradores
- o sistema diferencia perfis por papel
- usuários sem papel adequado não acessam essas funções

---

## Épico EP-02 — Pessoas

### HU-004 — Cadastrar pessoa
Como proprietário,  
quero cadastrar pessoas relacionadas ao meu inventário,  
para registrar de quem comprei, para quem vendi ou para quem emprestei itens.

#### Critérios de aceite
- pessoa possui nome e dados básicos
- pessoa pertence ao contexto do proprietário
- pessoa pode ser usada em movimentações

---

### HU-005 — Manter pessoa
Como proprietário,  
quero editar e consultar pessoas cadastradas,  
para manter o cadastro correto ao longo do tempo.

#### Critérios de aceite
- posso listar pessoas
- posso editar dados da pessoa
- o acesso respeita o proprietário logado

---

### HU-006 — Vincular pessoa a usuário
Como sistema,  
quero permitir vínculo entre pessoa e usuário,  
para autenticação e rastreabilidade quando necessário.

#### Critérios de aceite
- uma pessoa pode ter um usuário vinculado
- o vínculo pode ser consultado e mantido
- o relacionamento é consistente

---

## Épico EP-03 — Coisas

### HU-007 — Cadastrar coisa
Como proprietário,  
quero cadastrar uma coisa,  
para representar um item base do meu inventário.

#### Critérios de aceite
- a coisa possui nome
- a coisa pertence ao proprietário
- a coisa pode possuir descrição
- a coisa pode ser ativada ou inativada

---

### HU-008 — Manter coisa
Como proprietário,  
quero editar e consultar coisas cadastradas,  
para corrigir ou complementar seus dados.

#### Critérios de aceite
- posso listar coisas
- posso editar dados
- posso inativar sem perder histórico

---

### HU-009 — Usar coisas padrão
Como proprietário,  
quero começar a partir de coisas padrão com atributos pré-configurados,  
para acelerar meu cadastro inicial.

#### Critérios de aceite
- o sistema oferece modelos iniciais
- os modelos possuem atributos sugeridos
- o proprietário pode aproveitar ou adaptar o modelo

---

## Épico EP-04 — Instâncias

### HU-010 — Cadastrar instância individual
Como proprietário,  
quero cadastrar uma instância de uma coisa,  
para controlar uma unidade física específica.

#### Critérios de aceite
- a instância pertence a uma coisa
- a instância pode ter valor próprio
- a instância pode ter data de entrada própria
- a instância pode ter local atual

---

### HU-011 — Cadastrar múltiplas instâncias
Como proprietário,  
quero cadastrar múltiplas instâncias de uma só vez,  
para agilizar o registro de itens semelhantes.

#### Critérios de aceite
- posso informar uma quantidade
- posso definir dados comuns ao lote
- as instâncias permanecem individualmente rastreáveis

---

### HU-012 — Registrar valor estimado
Como proprietário,  
quero informar valor estimado para itens recebidos sem compra,  
para manter uma visão patrimonial mínima do inventário.

#### Critérios de aceite
- o sistema permite valor estimado
- presentes podem entrar sem valor pago
- o valor estimado fica armazenado na entrada ou instância, conforme modelagem adotada

---

## Épico EP-05 — Atributos dinâmicos

### HU-013 — Configurar atributos por coisa
Como proprietário,  
quero configurar atributos para uma coisa,  
para representar características específicas do item.

#### Critérios de aceite
- atributos podem ser texto, número ou booleano
- atributos ficam vinculados à coisa
- os atributos podem ser usados pelas instâncias

---

### HU-014 — Registrar atributos específicos da instância
Como proprietário,  
quero preencher atributos por instância,  
para refletir diferenças entre unidades físicas do mesmo item.

#### Critérios de aceite
- instâncias da mesma coisa podem ter valores diferentes
- o sistema respeita os tipos de dados definidos
- atributos não aplicáveis não precisam aparecer

---

### HU-015 — Suportar atributos em chave-valor
Como proprietário,  
quero registrar estruturas flexíveis em chave-valor,  
para armazenar acessórios e especificações mais livres.

#### Critérios de aceite
- o sistema suporta listas de chave-valor
- a estrutura fica ligada ao item correto
- os dados são persistidos com consistência

---

## Épico EP-06 — Locais

### HU-016 — Cadastrar local
Como proprietário,  
quero cadastrar locais,  
para indicar onde minhas instâncias estão guardadas.

#### Critérios de aceite
- o local possui nome
- o local pertence ao proprietário
- o local pode ser ativado ou inativado

---

### HU-017 — Organizar locais em hierarquia
Como proprietário,  
quero estruturar locais com sublocais,  
para representar melhor o espaço físico real.

#### Critérios de aceite
- um local pode ter pai
- posso consultar a hierarquia
- o sistema impede ciclos inválidos

---

### HU-018 — Definir local atual da instância
Como proprietário,  
quero informar onde uma instância está,  
para encontrá-la facilmente depois.

#### Critérios de aceite
- a instância possui um local atual
- o local é escolhido dentro do meu contexto
- a informação fica disponível para consulta

---

### HU-019 — Mover instância entre locais
Como proprietário,  
quero alterar o local atual de uma instância,  
para manter o inventário atualizado quando eu a mover fisicamente.

#### Critérios de aceite
- posso alterar o local atual
- o novo local deve ser válido
- a consistência do inventário é preservada

---

## Épico EP-07 — Movimentações

### HU-020 — Registrar entrada
Como proprietário,  
quero registrar a entrada de uma instância,  
para controlar quando e como ela passou a fazer parte do meu inventário.

#### Critérios de aceite
- a entrada registra data
- a entrada registra tipo ou origem
- a entrada pode registrar pessoa relacionada
- a entrada pode registrar valor pago ou estimado

---

### HU-021 — Registrar saída
Como proprietário,  
quero registrar a saída de uma instância,  
para controlar quando ela deixou de fazer parte do meu inventário ativo.

#### Critérios de aceite
- a saída registra data
- a saída registra motivo
- a saída pode registrar pessoa relacionada
- a instância deixa de estar disponível no inventário ativo

---

### HU-022 — Registrar empréstimo
Como proprietário,  
quero registrar empréstimo de uma instância,  
para saber com quem o item está e poder controlar sua devolução.

#### Critérios de aceite
- o empréstimo registra pessoa destinatária
- o empréstimo registra data
- o empréstimo pode registrar previsão de devolução
- a instância passa a estado compatível com empréstimo

---

### HU-023 — Registrar devolução
Como proprietário,  
quero registrar a devolução de um item emprestado,  
para que ele volte ao meu inventário disponível.

#### Critérios de aceite
- a devolução só ocorre para empréstimo ativo
- a devolução registra data
- a instância volta ao estado adequado

---

### HU-024 — Impedir inconsistências de estado
Como sistema,  
quero validar transições de estado das instâncias,  
para impedir operações inválidas.

#### Critérios de aceite
- item emprestado não pode ser emprestado novamente
- item com saída definitiva não pode continuar disponível
- operações inválidas são rejeitadas pela API

---

## Épico EP-08 — Segurança e contexto

### HU-025 — Restringir acesso por perfil
Como sistema,  
quero controlar acesso por papel,  
para garantir que cada usuário só execute ações permitidas.

#### Critérios de aceite
- administradores acessam gestão da plataforma
- proprietários acessam apenas seus recursos operacionais
- ações indevidas são bloqueadas

---

### HU-026 — Restringir acesso por proprietário
Como sistema,  
quero restringir todo dado ao contexto do proprietário correto,  
para garantir segregação e segurança dos dados.

#### Critérios de aceite
- consultas respeitam o proprietário logado
- comandos respeitam o proprietário logado
- o sistema não permite referência cruzada indevida entre proprietários
