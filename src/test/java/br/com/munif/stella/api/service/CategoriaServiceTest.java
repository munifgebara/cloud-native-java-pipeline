package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.CategoriaCreateDTO;
import br.com.munif.stella.api.dto.CategoriaUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.repository.CategoriaRepository;
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
    private CategoriaRepository repository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private CategoriaService service;

    @Test
    void deveCriarCategoriaNormalizandoCampos() {
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new CategoriaCreateDTO("  Eletronicos  ", "  Itens eletronicos  ", " eletronicos ", true));

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(repository).save(captor.capture());

        Categoria categoriaSalva = captor.getValue();
        assertThat(categoriaSalva.getNome()).isEqualTo("Eletronicos");
        assertThat(categoriaSalva.getDescricao()).isEqualTo("Itens eletronicos");
        assertThat(categoriaSalva.getIcone()).isEqualTo("eletronicos");
        assertThat(categoriaSalva.isAtivo()).isTrue();
        assertThat(resposta.nome()).isEqualTo("Eletronicos");
        assertThat(resposta.icone()).isEqualTo("eletronicos");
    }

    @Test
    void devePermitirCriarCategoriaInativa() {
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new CategoriaCreateDTO("Livros", null, null, false));

        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveAtualizarCategoriaComSituacao() {
        UUID id = UUID.randomUUID();
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome("Antiga");
        categoria.setDescricao("Descricao antiga");
        categoria.setIcone("livros");
        categoria.setAtivo(true);

        when(repository.findById(id)).thenReturn(Optional.of(categoria));
        when(repository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new CategoriaUpdateDTO("  Nova  ", "  Nova descricao  ", " moveis ", false));

        assertThat(resposta.nome()).isEqualTo("Nova");
        assertThat(resposta.descricao()).isEqualTo("Nova descricao");
        assertThat(resposta.icone()).isEqualTo("moveis");
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveRejeitarIconeForaDaListaControlada() {
        assertThatThrownBy(() -> service.criar(new CategoriaCreateDTO("Livros", null, "classe-css-livre", true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ícone");
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        Categoria categoria = new Categoria();
        categoria.setId(UUID.randomUUID());
        categoria.setNome("Livros");
        categoria.setDescricao(null);
        categoria.setAtivo(true);

        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Liv")).thenReturn(List.of(categoria));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Liv ")).hasSize(1);
    }
}
