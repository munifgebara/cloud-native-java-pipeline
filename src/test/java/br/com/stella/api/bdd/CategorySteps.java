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
    public void queExisteUmCadastroDeCategoriaComNomeEIcone(String nome, String icone) {
        request = new CategoryCreateDTO(nome, null, icone, true);
    }

    @When("the category is saved")
    public void aCategoriaForSalva() {
        response = service.create(request);
    }

    @Then("the registered category must be named {string}")
    public void aCategoriaCadastradaDeveSeChamar(String nome) {
        assertThat(response.nome()).isEqualTo(nome);
    }

    @Then("the icon of the registered category must be {string}")
    public void oIconeDaCategoriaCadastradaDeveSer(String icone) {
        assertThat(response.icone()).isEqualTo(icone);
    }

    @Then("the registered category must be active")
    public void aCategoriaCadastradaDeveEstarAtiva() {
        assertThat(response.ativa()).isTrue();
    }
}
