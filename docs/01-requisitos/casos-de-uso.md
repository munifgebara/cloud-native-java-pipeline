# Casos de Uso — Stella

## Objetivo
Descrever os principais casos de uso do Stella em nível funcional, de forma mais objetiva que as histórias de usuário.

---

# Atores

## Administrador
Usuário com responsabilidade de administração da plataforma.

## Proprietário
Usuário responsável por operar o próprio inventário.

## Sistema
Responsável por validar permissões, contexto e consistência das regras.

---

# UC-001 — Cadastrar proprietário

## Atores
- Administrador

## Objetivo
Criar um novo proprietário no sistema.

## Pré-condições
- usuário autenticado com papel de administrador

## Fluxo principal
1. O administrador acessa a funcionalidade de proprietários.
2. O administrador informa os dados do novo proprietário.
3. O sistema valida os dados.
4. O sistema cria o proprietário.
5. O sistema confirma o cadastro.

## Pós-condições
- proprietário criado e disponível para uso

---

# UC-002 — Manter proprietário

## Atores
- Administrador

## Objetivo
Editar ou inativar um proprietário existente.

## Pré-condições
- proprietário existente
- usuário autenticado com papel de administrador

## Fluxo principal
1. O administrador localiza o proprietário.
2. O administrador altera os dados desejados.
3. O sistema valida os dados.
4. O sistema persiste as alterações.

## Fluxos alternativos
- inativação do proprietário

## Pós-condições
- dados atualizados ou proprietário inativado

---

# UC-003 — Cadastrar administrador

## Atores
- Administrador

## Objetivo
Criar outro usuário com perfil de administrador.

## Pré-condições
- usuário autenticado com papel de administrador

## Fluxo principal
1. O administrador acessa a funcionalidade de usuários administrativos.
2. Informa os dados necessários.
3. Define o papel administrativo.
4. O sistema valida e cria o usuário.

## Pós-condições
- novo administrador cadastrado

---

# UC-004 — Cadastrar pessoa

## Atores
- Proprietário

## Objetivo
Registrar uma pessoa relacionada ao inventário do proprietário.

## Pré-condições
- usuário autenticado como proprietário

## Fluxo principal
1. O proprietário acessa a tela de pessoas.
2. Informa nome e demais dados.
3. O sistema valida os dados.
4. O sistema associa a pessoa ao proprietário.
5. O sistema salva o cadastro.

## Pós-condições
- pessoa cadastrada no contexto do proprietário

---

# UC-005 — Manter pessoa

## Atores
- Proprietário

## Objetivo
Editar ou consultar pessoas relacionadas ao inventário.

## Pré-condições
- pessoa existente no contexto do proprietário

## Fluxo principal
1. O proprietário consulta a lista de pessoas.
2. Seleciona uma pessoa.
3. Edita os dados necessários.
4. O sistema valida e salva.

## Pós-condições
- pessoa atualizada

---

# UC-006 — Vincular pessoa a usuário

## Atores
- Administrador
- Proprietário, conforme política futura

## Objetivo
Relacionar uma pessoa a um usuário do sistema.

## Pré-condições
- pessoa existente
- usuário existente ou a ser criado

## Fluxo principal
1. O ator autorizado seleciona uma pessoa.
2. Seleciona ou cria o usuário correspondente.
3. O sistema registra o vínculo.

## Pós-condições
- pessoa vinculada a usuário

---

# UC-007 — Cadastrar coisa

## Atores
- Proprietário

## Objetivo
Criar um item base do inventário.

## Pré-condições
- usuário autenticado no contexto do proprietário

## Fluxo principal
1. O proprietário acessa a funcionalidade de coisas.
2. Informa nome, descrição e dados complementares.
3. O sistema valida os dados.
4. O sistema cria a coisa.

## Pós-condições
- coisa cadastrada

---

# UC-008 — Manter coisa

## Atores
- Proprietário

## Objetivo
Editar ou inativar uma coisa.

## Pré-condições
- coisa existente

## Fluxo principal
1. O proprietário localiza a coisa.
2. Edita os dados.
3. O sistema valida e salva.

## Fluxos alternativos
- inativação da coisa

## Pós-condições
- coisa atualizada ou inativada

---

# UC-009 — Criar coisa a partir de modelo padrão

## Atores
- Proprietário

## Objetivo
Criar uma coisa usando um modelo já pré-configurado.

## Pré-condições
- existência de modelo padrão disponível

## Fluxo principal
1. O proprietário escolhe criar uma coisa a partir de um modelo.
2. O sistema exibe modelos disponíveis.
3. O proprietário seleciona um modelo.
4. O sistema pré-preenche atributos e informações sugeridas.
5. O proprietário confirma ou ajusta os dados.
6. O sistema cria a coisa.

## Pós-condições
- coisa criada com base em modelo padrão

---

# UC-010 — Cadastrar instância individual

## Atores
- Proprietário

## Objetivo
Registrar uma unidade física específica de uma coisa.

## Pré-condições
- coisa existente

## Fluxo principal
1. O proprietário seleciona uma coisa.
2. Escolhe cadastrar uma instância.
3. Informa dados da unidade física.
4. O sistema valida.
5. O sistema cria a instância.

## Pós-condições
- instância criada e rastreável

---

# UC-011 — Cadastrar múltiplas instâncias

## Atores
- Proprietário

## Objetivo
Registrar várias unidades físicas semelhantes de uma mesma coisa.

## Pré-condições
- coisa existente

## Fluxo principal
1. O proprietário seleciona uma coisa.
2. Informa quantidade de instâncias.
3. Informa dados comuns ao lote.
4. O sistema gera as instâncias individualmente.
5. O sistema confirma o cadastro.

## Fluxos alternativos
- permitir ajuste individual posterior
- permitir valores e dados parcialmente diferentes, conforme modelagem adotada

## Pós-condições
- múltiplas instâncias criadas

---

# UC-012 — Registrar entrada

## Atores
- Proprietário

## Objetivo
Registrar como uma instância passou a compor o inventário.

## Pré-condições
- instância existente ou sendo criada no fluxo
- pessoa relacionada opcionalmente existente

## Fluxo principal
1. O proprietário informa os dados da entrada.
2. Define tipo de entrada.
3. Informa data e valor pago ou estimado.
4. Informa pessoa de origem, se aplicável.
5. O sistema valida e grava a entrada.

## Pós-condições
- entrada registrada
- instância passa a existir no inventário ativo

---

# UC-013 — Registrar saída

## Atores
- Proprietário

## Objetivo
Registrar a saída definitiva de uma instância do inventário.

## Pré-condições
- instância ativa e disponível para saída

## Fluxo principal
1. O proprietário seleciona a instância.
2. Informa o tipo de saída.
3. Informa data e demais dados.
4. O sistema valida as regras.
5. O sistema registra a saída.
6. O sistema altera o estado da instância.

## Pós-condições
- saída registrada
- instância deixa de estar disponível no inventário ativo

---

# UC-014 — Registrar empréstimo

## Atores
- Proprietário

## Objetivo
Registrar que uma instância foi emprestada a uma pessoa.

## Pré-condições
- instância disponível
- pessoa destinatária cadastrada

## Fluxo principal
1. O proprietário seleciona a instância.
2. Informa a pessoa destinatária.
3. Informa data do empréstimo.
4. Informa previsão de devolução, se desejar.
5. O sistema valida a disponibilidade da instância.
6. O sistema registra o empréstimo.
7. O sistema altera o estado da instância.

## Pós-condições
- empréstimo ativo registrado

---

# UC-015 — Registrar devolução

## Atores
- Proprietário

## Objetivo
Registrar o retorno de um item emprestado.

## Pré-condições
- empréstimo ativo existente

## Fluxo principal
1. O proprietário localiza o empréstimo ativo.
2. Informa a devolução.
3. O sistema registra a data de devolução.
4. O sistema encerra o empréstimo.
5. O sistema ajusta o estado da instância.

## Pós-condições
- empréstimo encerrado
- instância novamente disponível, salvo outra regra específica

---

# UC-016 — Cadastrar local

## Atores
- Proprietário

## Objetivo
Registrar um local físico de armazenamento.

## Pré-condições
- usuário autenticado como proprietário

## Fluxo principal
1. O proprietário acessa a funcionalidade de locais.
2. Informa nome e dados do local.
3. O sistema valida.
4. O sistema salva.

## Pós-condições
- local cadastrado

---

# UC-017 — Organizar hierarquia de locais

## Atores
- Proprietário

## Objetivo
Criar uma relação pai-filho entre locais.

## Pré-condições
- locais existentes no contexto do proprietário

## Fluxo principal
1. O proprietário seleciona um local.
2. Define um local pai.
3. O sistema valida ausência de ciclo.
4. O sistema persiste a hierarquia.

## Pós-condições
- hierarquia de locais atualizada

---

# UC-018 — Definir local atual da instância

## Atores
- Proprietário

## Objetivo
Informar onde uma instância está guardada.

## Pré-condições
- instância existente
- local existente

## Fluxo principal
1. O proprietário seleciona a instância.
2. Escolhe o local atual.
3. O sistema valida o contexto do local.
4. O sistema registra o local atual.

## Pós-condições
- instância vinculada ao local

---

# UC-019 — Mover instância entre locais

## Atores
- Proprietário

## Objetivo
Alterar o local atual de uma instância.

## Pré-condições
- instância existente
- novo local válido

## Fluxo principal
1. O proprietário localiza a instância.
2. Solicita alteração de local.
3. Seleciona o novo local.
4. O sistema valida o contexto.
5. O sistema atualiza o local atual.

## Pós-condições
- local atual da instância atualizado

---

# UC-020 — Configurar atributos de uma coisa

## Atores
- Proprietário

## Objetivo
Definir quais atributos uma coisa possui.

## Pré-condições
- coisa existente

## Fluxo principal
1. O proprietário acessa a configuração da coisa.
2. Adiciona atributos.
3. Define nome, tipo e obrigatoriedade.
4. O sistema valida e salva.

## Pós-condições
- atributos configurados para a coisa

---

# UC-021 — Preencher atributos da instância

## Atores
- Proprietário

## Objetivo
Informar os valores concretos dos atributos de uma instância.

## Pré-condições
- instância existente
- definições de atributos existentes

## Fluxo principal
1. O proprietário abre a instância.
2. Visualiza os atributos aplicáveis.
3. Informa os valores.
4. O sistema valida os tipos.
5. O sistema salva os valores.

## Pós-condições
- atributos da instância preenchidos

---

# UC-022 — Consultar inventário por proprietário

## Atores
- Proprietário
- Administrador, se houver política específica futura

## Objetivo
Visualizar o inventário do contexto atual.

## Pré-condições
- dados existentes no contexto do proprietário

## Fluxo principal
1. O usuário acessa a visão de inventário.
2. O sistema lista as coisas e suas instâncias, conforme a consulta adotada.
3. O usuário pode filtrar e navegar.

## Pós-condições
- inventário apresentado ao usuário

---

# UC-023 — Consultar inventário por local

## Atores
- Proprietário

## Objetivo
Visualizar itens com base na estrutura de locais.

## Pré-condições
- locais existentes
- instâncias vinculadas a locais

## Fluxo principal
1. O proprietário acessa a visão por local.
2. Seleciona um local.
3. O sistema mostra as instâncias naquele local e, se aplicável, em sublocais.

## Pós-condições
- inventário filtrado por localização

---

# UC-024 — Consultar histórico de instância

## Atores
- Proprietário

## Objetivo
Visualizar o histórico de vida de uma instância.

## Pré-condições
- instância existente

## Fluxo principal
1. O proprietário localiza a instância.
2. Solicita a visualização do histórico.
3. O sistema apresenta entrada, movimentações, empréstimos, devoluções, saídas e alterações relevantes.

## Pós-condições
- histórico exibido para consulta
