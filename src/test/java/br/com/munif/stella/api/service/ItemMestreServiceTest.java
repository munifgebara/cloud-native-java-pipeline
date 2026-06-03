package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMestreServiceTest {

    @Mock
    private ItemMestreRepository repository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemMestreService service;

    @Test
    void deveCriarItemMestreComCategoriaNormalizandoCampos() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Eletronicos", "eletronicos", true);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemMestreCreateDTO(
                "  Notebook Dell Latitude 5440  ",
                "  Notebook corporativo  ",
                "  Preparado para futuras instancias  ",
                categoriaId,
                true
        ));

        ArgumentCaptor<ItemMestre> captor = ArgumentCaptor.forClass(ItemMestre.class);
        verify(repository).save(captor.capture());

        ItemMestre itemSalvo = captor.getValue();
        assertThat(itemSalvo.getNome()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(itemSalvo.getDescricao()).isEqualTo("Notebook corporativo");
        assertThat(itemSalvo.getObservacoes()).isEqualTo("Preparado para futuras instancias");
        assertThat(itemSalvo.getCategoria()).isEqualTo(categoria);
        assertThat(resposta.categoriaId()).isEqualTo(categoriaId);
        assertThat(resposta.categoriaNome()).isEqualTo("Eletronicos");
        assertThat(resposta.categoriaIcone()).isEqualTo("eletronicos");
    }

    @Test
    void deveCriarItemMestreSemCategoria() {
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemMestreCreateDTO("Cadeira ergonomica", null, null, null, true));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        assertThat(resposta.categoriaId()).isNull();
        verify(categoriaRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveAtualizarItemMestreComCategoria() {
        UUID id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        ItemMestre item = item(id, "Antigo", null);
        Categoria categoria = categoria(categoriaId, "Moveis", "moveis", true);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));
        when(repository.save(any(ItemMestre.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new ItemMestreUpdateDTO(
                "  Cadeira ergonomica  ",
                "  Cadeira de escritorio  ",
                null,
                categoriaId,
                false
        ));

        assertThat(resposta.nome()).isEqualTo("Cadeira ergonomica");
        assertThat(resposta.descricao()).isEqualTo("Cadeira de escritorio");
        assertThat(resposta.categoriaNome()).isEqualTo("Moveis");
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveImpedirCategoriaInativaNoItemMestre() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Inativa", null, false);

        when(categoriaRepository.findById(categoriaId)).thenReturn(Optional.of(categoria));

        assertThatThrownBy(() -> service.criar(new ItemMestreCreateDTO("Furadeira", null, null, categoriaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Categoria deve estar ativa");

        verify(repository, never()).save(any(ItemMestre.class));
    }

    @Test
    void deveBuscarPorNomeSomenteQuandoFiltroInformado() {
        ItemMestre item = item(UUID.randomUUID(), "Furadeira Bosch", null);

        when(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc("Furadeira")).thenReturn(List.of(item));

        assertThat(service.buscarPorNome("  ")).isEmpty();
        assertThat(service.buscarPorNome(" Furadeira ")).hasSize(1);
    }

    @Test
    void deveFiltrarPorNomeECategoria() {
        UUID categoriaId = UUID.randomUUID();
        Categoria categoria = categoria(categoriaId, "Eletronicos", "eletronicos", true);
        ItemMestre item = item(UUID.randomUUID(), "Notebook", categoria);

        when(repository.filtrarAtivos("Notebook", categoriaId)).thenReturn(List.of(item));

        var resposta = service.filtrar(" Notebook ", categoriaId);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().nome()).isEqualTo("Notebook");
    }

    private ItemMestre item(UUID id, String nome, Categoria categoria) {
        ItemMestre item = new ItemMestre();
        item.setId(id);
        item.setNome(nome);
        item.setCategoria(categoria);
        item.setAtivo(true);
        return item;
    }

    private Categoria categoria(UUID id, String nome, String icone, boolean ativa) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setIcone(icone);
        categoria.setAtivo(ativa);
        return categoria;
    }
}
