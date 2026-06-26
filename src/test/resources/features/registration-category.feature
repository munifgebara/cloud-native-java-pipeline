Feature: Owner scoped category registration
  As a Stella user
  I want to register inventory categories
  In order to organise only the inventory data I can access

  Scenario: Register a private category for the authenticated owner
    Given "Owner A" is using Stella
    And a private category registration exists with name " Books " and icon " livros "
    When the category is saved
    Then the registered category must be named "Books"
    And the icon of the registered category must be "livros"
    And the registered category must be active
    And the category must belong to "Owner A"
    And the category must stay private to its owner

  Scenario: Public categories are readable by another owner
    Given "Owner A" is using Stella
    And a public category registration exists with name " Shared tools " and icon " ferramentas "
    When the category is saved
    Then the registered category must be named "Shared tools"
    And the category must belong to "Owner A"
    And the category must be readable by other owners
    When "Owner B" lists active categories
    Then "Shared tools" must be visible

  Scenario: Private categories are hidden from another owner
    Given "Owner A" is using Stella
    And a private category registration exists with name " Private documents " and icon " documentos "
    When the category is saved
    When "Owner B" lists active categories
    Then "Private documents" must not be visible
