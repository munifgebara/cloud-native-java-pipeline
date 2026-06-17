package br.com.stella.api.service;

import br.com.stella.api.dto.CategoryCreateDTO;
import br.com.stella.api.dto.CategoryUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoryRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CategoryService service;

    @Test
    void deveCriarCategoriaNormalizandoCampos() {
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new CategoryCreateDTO("  Eletronicos  ", "  Items eletronicos  ", " eletronicos ", true));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).save(captor.capture());

        Category categoriaSalva = captor.getValue();
        assertThat(categoriaSalva.getName()).isEqualTo("Eletronicos");
        assertThat(categoriaSalva.getDescription()).isEqualTo("Items eletronicos");
        assertThat(categoriaSalva.getIcon()).isEqualTo("eletronicos");
        assertThat(categoriaSalva.isActive()).isTrue();
        assertThat(resposta.nome()).isEqualTo("Eletronicos");
        assertThat(resposta.icone()).isEqualTo("eletronicos");
    }

    @Test
    void devePermitirCriarCategoriaInativa() {
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new CategoryCreateDTO("Livros", null, null, false));

        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveAtualizarCategoriaComSituacao() {
        UUID id = UUID.randomUUID();
        Category category = new Category();
        category.setId(id);
        category.setName("Antiga");
        category.setDescription("Descricao antiga");
        category.setIcon("livros");
        category.setActive(true);

        when(repository.findById(id)).thenReturn(Optional.of(category));
        when(repository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new CategoryUpdateDTO("  Nova  ", "  Nova descricao  ", " moveis ", false));

        assertThat(resposta.nome()).isEqualTo("Nova");
        assertThat(resposta.descricao()).isEqualTo("Nova descricao");
        assertThat(resposta.icone()).isEqualTo("moveis");
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveRejeitarIconeForaDaListaControlada() {
        assertThatThrownBy(() -> service.criar(new CategoryCreateDTO("Livros", null, "classe-css-livre", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid category icon");
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Livros");
        category.setDescription(null);
        category.setActive(true);

        when(repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("Liv")).thenReturn(List.of(category));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Liv ")).hasSize(1);
    }
}
