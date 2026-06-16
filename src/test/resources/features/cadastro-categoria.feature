Feature: Category registration
  As a Stella user
  I want to register inventory categories
  In order to organise registered main items

  Scenario: Register category with valid data
    Given that a category registration exists with name " Books " and icon " books "
    When the category is saved
    Then the registered category must be named "Books"
    And the icon of the registered category must be "books"
    And the registered category must be active
