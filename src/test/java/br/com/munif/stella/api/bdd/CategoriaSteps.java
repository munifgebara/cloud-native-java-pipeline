package br.com.munif.stella.api.bdd;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.service.CategoriaService;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoriaSteps {

    private CategoriaRepository repository;
    private CategoriaService service;
    private CategoriaCreateDTO request;
    private CategoriaResponseDTO response;

    @Before
    public void setUp() {
        repository = mock(CategoriaRepository.class);
        EntityManager entityManager = mock(EntityManager.class);
        service = new CategoriaService(repository, entityManager);

        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Given("that a category registration exists with name {string} and icon {string}")
    public void queExisteUmCadastroDeCategoriaComNomeEIcone(String nome, String icone) {
        request = new CategoriaCreateDTO(nome, null, icone, true);
    }

    @When("the category is saved")
    public void aCategoriaForSalva() {
        response = service.criar(request);
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
