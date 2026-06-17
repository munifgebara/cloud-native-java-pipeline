package br.com.stella.api.bdd;

import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryResponseDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.repository.CategoryRepository;
import br.com.stella.api.service.CategoryService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategorySteps {

    private CategoryRepository repository;
    private CategoryService service;
    private CategoryCreateDTO request;
    private CategoryResponseDTO response;

    @Before
    public void setUp() {
        repository = mock(CategoryRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        service = new CategoryService(repository, entityManager);

        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Given("that a category registration exists with name {string} and icon {string}")
    public void categoryRegistrationExistsWithNameAndIcon(String name, String icon) {
        request = new CategoryCreateDTO(name, null, icon, true);
    }

    @When("the category is saved")
    public void categoryIsSaved() {
        response = service.create(request);
    }

    @Then("the registered category must be named {string}")
    public void registeredCategoryShouldBeNamed(String name) {
        assertThat(response.name()).isEqualTo(name);
    }

    @Then("the icon of the registered category must be {string}")
    public void registeredCategoryIconShouldBe(String icon) {
        assertThat(response.icon()).isEqualTo(icon);
    }

    @Then("the registered category must be active")
    public void registeredCategoryShouldBeActive() {
        assertThat(response.ativa()).isTrue();
    }
}
