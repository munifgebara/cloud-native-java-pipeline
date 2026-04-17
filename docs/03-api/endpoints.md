# Endpoints — Stella

## Objetivo
Descrever a proposta inicial de endpoints REST do Stella.

## Observações gerais
- todos os endpoints protegidos devem respeitar autenticação e autorização
- os endpoints operacionais devem respeitar o contexto do proprietário
- os payloads abaixo são propostas iniciais e podem evoluir
- sempre que possível, respostas devem ser consistentes entre recursos

---

# Convenções sugeridas

## Base path
`/api`

## Formato
`application/json`

## Identificadores
Preferencialmente `UUID` ou identificador estável equivalente adotado pelo projeto.

## Padrão sugerido de resposta de erro
```json
{
  "timestamp": "2026-04-17T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Mensagem de validação",
  "path": "/api/coisas"
}


1. Proprietários
GET /api/proprietarios

Lista proprietários.

Acesso
ADMIN
Resposta exemplo
[
  {
    "id": "1",
    "nome": "Munif",
    "ativo": true
  }
]
POST /api/proprietarios

Cria um proprietário.

Acesso
ADMIN
Request exemplo
{
  "nome": "Munif",
  "observacao": "Proprietário principal"
}
Response exemplo
{
  "id": "1",
  "nome": "Munif",
  "ativo": true,
  "observacao": "Proprietário principal"
}
GET /api/proprietarios/{id}

Obtém detalhes de um proprietário.

Acesso
ADMIN
PUT /api/proprietarios/{id}

Atualiza um proprietário.

Acesso
ADMIN
Request exemplo
{
  "nome": "Munif Gebara",
  "observacao": "Cadastro atualizado"
}
PATCH /api/proprietarios/{id}/inativar

Inativa um proprietário.

Acesso
ADMIN
2. Pessoas
GET /api/pessoas

Lista pessoas do proprietário atual.

Acesso
PROPRIETARIO
Query params sugeridos
q
ativo
page
size
Response exemplo
[
  {
    "id": "10",
    "nome": "João da Silva",
    "email": "joao@email.com",
    "telefone": "44999999999",
    "ativo": true
  }
]
POST /api/pessoas

Cria uma pessoa no contexto do proprietário.

Acesso
PROPRIETARIO
Request exemplo
{
  "nome": "João da Silva",
  "email": "joao@email.com",
  "telefone": "44999999999",
  "observacao": "Fornecedor de livros"
}
GET /api/pessoas/{id}

Obtém uma pessoa do contexto atual.

Acesso
PROPRIETARIO
PUT /api/pessoas/{id}

Atualiza uma pessoa.

Acesso
PROPRIETARIO
PATCH /api/pessoas/{id}/inativar

Inativa uma pessoa.

Acesso
PROPRIETARIO
PUT /api/pessoas/{id}/usuario

Vincula ou altera o usuário relacionado à pessoa.

Acesso
ADMIN ou regra específica futura
Request exemplo
{
  "usuarioId": "200"
}
3. Coisas
GET /api/coisas

Lista coisas do proprietário.

Acesso
PROPRIETARIO
Query params sugeridos
q
categoria
ativo
page
size
Response exemplo
[
  {
    "id": "100",
    "nome": "Livro",
    "descricao": "Livro físico",
    "categoria": "LEITURA",
    "ativo": true
  }
]
POST /api/coisas

Cria uma coisa.

Acesso
PROPRIETARIO
Request exemplo
{
  "nome": "Prato tipo A porcelana multicolorido",
  "descricao": "Prato de jantar",
  "categoria": "COZINHA"
}
GET /api/coisas/{id}

Obtém detalhes de uma coisa.

Acesso
PROPRIETARIO
PUT /api/coisas/{id}

Atualiza uma coisa.

Acesso
PROPRIETARIO
PATCH /api/coisas/{id}/inativar

Inativa uma coisa.

Acesso
PROPRIETARIO
POST /api/coisas/modelos/{modeloId}

Cria uma coisa a partir de um modelo padrão.

Acesso
PROPRIETARIO
Request exemplo
{
  "nome": "Coleção de livros de ficção"
}
4. Instâncias
GET /api/instancias

Lista instâncias do proprietário.

Acesso
PROPRIETARIO
Query params sugeridos
coisaId
situacao
localId
q
page
size
Response exemplo
[
  {
    "id": "500",
    "coisaId": "100",
    "coisaNome": "Prato tipo A porcelana multicolorido",
    "situacao": "ATIVA",
    "dataEntrada": "2026-04-17",
    "valorAquisicao": 20.00,
    "valorEstimado": null,
    "localAtualId": "900",
    "localAtualNome": "Cozinha > Armário > Prateleira 1"
  }
]
POST /api/instancias

Cria uma instância individual.

Acesso
PROPRIETARIO
Request exemplo
{
  "coisaId": "100",
  "codigoInterno": "PRATO-001",
  "dataEntrada": "2026-04-17",
  "valorAquisicao": 20.00,
  "valorEstimado": null,
  "localAtualId": "900",
  "observacao": "Prato azul"
}
POST /api/instancias/lote

Cria múltiplas instâncias.

Acesso
PROPRIETARIO
Request exemplo
{
  "coisaId": "100",
  "quantidade": 12,
  "dadosComuns": {
    "dataEntrada": "2026-04-17",
    "localAtualId": "900"
  },
  "instancias": [
    {
      "valorAquisicao": 20.00,
      "observacao": "Verde"
    },
    {
      "valorAquisicao": 21.00,
      "observacao": "Azul"
    }
  ]
}
Observação

A modelagem final pode optar por:

dados totalmente homogêneos
dados parcialmente específicos por instância
ambos
GET /api/instancias/{id}

Obtém detalhes de uma instância.

Acesso
PROPRIETARIO
PUT /api/instancias/{id}

Atualiza dados editáveis da instância.

Acesso
PROPRIETARIO
PATCH /api/instancias/{id}/local

Altera o local atual da instância.

Acesso
PROPRIETARIO
Request exemplo
{
  "localAtualId": "901"
}
GET /api/instancias/{id}/historico

Obtém histórico consolidado da instância.

Acesso
PROPRIETARIO
Response exemplo
{
  "instanciaId": "500",
  "coisaNome": "Livro",
  "eventos": [
    {
      "tipo": "ENTRADA",
      "data": "2026-04-10",
      "descricao": "Compra"
    },
    {
      "tipo": "EMPRESTIMO",
      "data": "2026-04-12",
      "descricao": "Emprestado para João"
    },
    {
      "tipo": "DEVOLUCAO",
      "data": "2026-04-16",
      "descricao": "Devolvido"
    }
  ]
}
5. Entradas
POST /api/entradas

Registra entrada de instância.

Acesso
PROPRIETARIO
Request exemplo
{
  "instanciaCoisaId": "500",
  "tipoEntrada": "COMPRA",
  "dataEntrada": "2026-04-17",
  "valorPago": 20.00,
  "valorEstimado": null,
  "pessoaOrigemId": "10",
  "observacao": "Compra em loja"
}
POST /api/entradas/lote

Registra entradas em lote.

Acesso
PROPRIETARIO
Request exemplo
{
  "coisaId": "100",
  "tipoEntrada": "COMPRA",
  "dataEntrada": "2026-04-17",
  "pessoaOrigemId": "10",
  "itens": [
    {
      "valorPago": 20.00,
      "observacao": "Prato verde"
    },
    {
      "valorPago": 21.00,
      "observacao": "Prato azul"
    }
  ]
}
GET /api/entradas/{id}

Obtém detalhes de uma entrada.

Acesso
PROPRIETARIO
6. Saídas
POST /api/saidas

Registra saída definitiva de uma instância.

Acesso
PROPRIETARIO
Request exemplo
{
  "instanciaCoisaId": "500",
  "tipoSaida": "VENDA",
  "dataSaida": "2026-04-17",
  "valorRecebido": 35.00,
  "pessoaDestinoId": "12",
  "observacao": "Venda particular"
}
GET /api/saidas/{id}

Obtém detalhes de uma saída.

Acesso
PROPRIETARIO
7. Empréstimos
POST /api/emprestimos

Registra empréstimo de instância.

Acesso
PROPRIETARIO
Request exemplo
{
  "instanciaCoisaId": "500",
  "pessoaDestinoId": "11",
  "dataEmprestimo": "2026-04-17",
  "dataPrevistaDevolucao": "2026-04-24",
  "observacao": "Emprestado ao João"
}
POST /api/emprestimos/{id}/devolucao

Registra devolução do empréstimo.

Acesso
PROPRIETARIO
Request exemplo
{
  "dataDevolucao": "2026-04-24",
  "observacao": "Devolvido em bom estado"
}
GET /api/emprestimos/{id}

Obtém detalhes de um empréstimo.

Acesso
PROPRIETARIO
GET /api/emprestimos

Lista empréstimos do proprietário.

Acesso
PROPRIETARIO
Query params sugeridos
status
pessoaId
page
size
8. Locais
GET /api/locais

Lista locais do proprietário.

Acesso
PROPRIETARIO
Query params sugeridos
paiId
ativo
page
size
POST /api/locais

Cria um local.

Acesso
PROPRIETARIO
Request exemplo
{
  "nome": "Casa em Maringá",
  "descricao": "Residência principal",
  "localPaiId": null
}
GET /api/locais/{id}

Obtém detalhes de um local.

Acesso
PROPRIETARIO
PUT /api/locais/{id}

Atualiza um local.

Acesso
PROPRIETARIO
PATCH /api/locais/{id}/inativar

Inativa um local.

Acesso
PROPRIETARIO
GET /api/locais/arvore

Retorna estrutura hierárquica de locais.

Acesso
PROPRIETARIO
Response exemplo
[
  {
    "id": "1",
    "nome": "Casa em Maringá",
    "filhos": [
      {
        "id": "2",
        "nome": "Quarto do casal",
        "filhos": [
          {
            "id": "3",
            "nome": "Closet",
            "filhos": [
              {
                "id": "4",
                "nome": "Prateleira 1",
                "filhos": []
              }
            ]
          }
        ]
      }
    ]
  }
]
9. Atributos dinâmicos
GET /api/coisas/{id}/atributos

Lista definições de atributos de uma coisa.

Acesso
PROPRIETARIO
POST /api/coisas/{id}/atributos

Cria definição de atributo para uma coisa.

Acesso
PROPRIETARIO
Request exemplo
{
  "nome": "cor",
  "tipo": "TEXTO",
  "obrigatorio": false,
  "multiplo": false,
  "ordem": 1
}
PUT /api/coisas/{id}/atributos/{atributoId}

Atualiza definição de atributo.

Acesso
PROPRIETARIO
DELETE /api/coisas/{id}/atributos/{atributoId}

Remove definição de atributo, se permitido pela regra de negócio.

Acesso
PROPRIETARIO
GET /api/instancias/{id}/atributos

Lista valores de atributos da instância.

Acesso
PROPRIETARIO
PUT /api/instancias/{id}/atributos

Atualiza valores de atributos da instância.

Acesso
PROPRIETARIO
Request exemplo
{
  "atributos": [
    {
      "definicaoAtributoId": "1",
      "valorTexto": "Azul"
    },
    {
      "definicaoAtributoId": "2",
      "valorNumero": 32
    }
  ],
  "atributosChaveValor": [
    {
      "chave": "acessorio",
      "tipoValor": "TEXTO",
      "valorTexto": "capa protetora"
    }
  ]
}
10. Catálogo de modelos
GET /api/modelos-coisa

Lista modelos padrão disponíveis.

Acesso
PROPRIETARIO
GET /api/modelos-coisa/{id}

Obtém detalhes de um modelo padrão.

Acesso
PROPRIETARIO
11. Consultas agregadas
GET /api/inventario

Consulta inventário do proprietário.

Acesso
PROPRIETARIO
Query params sugeridos
coisaId
localId
situacao
q
page
size
GET /api/inventario/por-local

Consulta inventário agrupado por local.

Acesso
PROPRIETARIO
GET /api/dashboard/resumo

Obtém indicadores resumidos do inventário.

Acesso
PROPRIETARIO
Response exemplo
{
  "totalCoisas": 30,
  "totalInstanciasAtivas": 180,
  "totalEmprestadas": 7,
  "totalLocais": 15
}
12. Regras transversais da API
Segurança
autenticação obrigatória nos recursos protegidos
autorização por papel
autorização por contexto do proprietário
Validações
integridade do estado da instância
integridade da hierarquia de locais
consistência entre dono do recurso e dono do contexto autenticado
Auditoria
alterações relevantes devem ser auditáveis
quando possível, aproveitar infraestrutura já existente no projeto
Exclusão
preferir inativação lógica em entidades com histórico
