package br.com.munif.stella.api.bdd;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaResponseDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.service.CategoriaService;
import io.cucumber.java.Before;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
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

    @Dado("que existe um cadastro de categoria com nome {string} e icone {string}")
    public void queExisteUmCadastroDeCategoriaComNomeEIcone(String nome, String icone) {
        request = new CategoriaCreateDTO(nome, null, icone, true);
    }

    @Quando("a categoria for salva")
    public void aCategoriaForSalva() {
        response = service.criar(request);
    }

    @Entao("a categoria cadastrada deve se chamar {string}")
    public void aCategoriaCadastradaDeveSeChamar(String nome) {
        assertThat(response.nome()).isEqualTo(nome);
    }

    @Entao("o icone da categoria cadastrada deve ser {string}")
    public void oIconeDaCategoriaCadastradaDeveSer(String icone) {
        assertThat(response.icone()).isEqualTo(icone);
    }

    @Entao("a categoria cadastrada deve estar ativa")
    public void aCategoriaCadastradaDeveEstarAtiva() {
        assertThat(response.ativa()).isTrue();
    }
}
