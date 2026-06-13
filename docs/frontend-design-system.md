# Stella Frontend Design System

Este documento registra a base visual do frontend do Stella. Ele vale apenas para Angular/PrimeNG e nao altera backend, contratos de API ou regras de negocio.

## Principios

- Interface operacional, limpa e previsivel.
- Base neutra para conteudo, com cor usada por papel semantico.
- PrimeNG continua sendo a biblioteca principal; o Stella adiciona tokens e classes de produto por cima.
- Recursos de IA, como busca vetorial e cadastro por foto, usam tratamento visual proprio, sem competir com erro, sucesso ou alerta.

## Tokens

Os tokens globais ficam em `frontend/src/styles.css`.

- `--stella-color-bg`: fundo da aplicacao.
- `--stella-color-surface`: cards, tabelas e paineis.
- `--stella-color-surface-muted`: areas neutras e placeholders visuais.
- `--stella-color-border`: divisorias e bordas comuns.
- `--stella-color-text`: texto principal.
- `--stella-color-text-muted`: subtitulos, metadados e labels auxiliares.
- `--stella-color-brand`: marca, navegacao ativa e estrutura.
- `--stella-color-info`: consulta, informacao e acoes operacionais.
- `--stella-color-success`: sucesso, concluido e disponivel.
- `--stella-color-warning`: atencao, pendente e incompleto.
- `--stella-color-danger`: erro, falha, exclusao e bloqueio.
- `--stella-color-ai`: IA, busca vetorial, sugestoes e descoberta.

## Regras de cor

- Nao usar hex direto em CSS novo quando houver token equivalente.
- Nao usar verde para acao primaria generica; verde e reservado para sucesso.
- Nao usar roxo fora de IA/descoberta.
- Nao usar vermelho para texto comum ou alerta leve; vermelho e apenas erro, exclusao ou bloqueio.
- Cor nao deve ser a unica comunicacao de estado; sempre combinar com texto, icone ou label.

## Componentes CSS

- `.page`: estrutura vertical padrao de tela.
- `.page-header`: titulo, subtitulo e acao primaria.
- `.toolbar` ou `.stella-toolbar`: busca, filtros e acoes secundarias.
- `.stella-panel`: painel padrao com borda, superficie e sombra leve.
- `.stella-card`: card simples para conteudo repetido.
- `.stella-ai-panel`: bloco para IA, busca vetorial, cadastro por foto e sugestoes.
- `.error-box` e `.success-box`: mensagens de estado ja padronizadas.
- `.stella-state--info`, `.stella-state--warning`, `.stella-state--error`, `.stella-state--success`: estados semanticos adicionais.

## Aplicacao inicial

A primeira aplicacao dos tokens cobre:

- shell da aplicacao;
- sidebar, topbar e navegacao mobile;
- login;
- dashboard;
- busca vetorial do dashboard;
- listagem e busca semantica de itens mestre;
- cadastro por foto.

## Proximos passos recomendados

- Migrar CSS local restante para tokens quando as telas forem alteradas.
- Extrair componentes Angular compartilhados se os mesmos padroes continuarem se repetindo.
- Revisar mobile de listas longas com cards dedicados, principalmente em itens e instancias.
- Considerar modo escuro somente depois da migracao completa para tokens.
