# language: pt
Funcionalidade: Cadastro de categoria
  Como usuario do Stella
  Quero cadastrar categorias de inventario
  Para organizar os itens mestre cadastrados

  Cenario: Cadastrar categoria com dados validos
    Dado que existe um cadastro de categoria com nome " Livros " e icone " livros "
    Quando a categoria for salva
    Entao a categoria cadastrada deve se chamar "Livros"
    E o icone da categoria cadastrada deve ser "livros"
    E a categoria cadastrada deve estar ativa
