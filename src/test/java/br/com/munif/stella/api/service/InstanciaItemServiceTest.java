package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
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
class InstanciaItemServiceTest {

    @Mock
    private InstanciaItemRepository repository;

    @Mock
    private ItemMestreRepository itemMestreRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InstanciaItemService service;

    @Test
    void deveCriarInstanciaComItemMestreNormalizandoCampos() {
        UUID itemMestreId = UUID.randomUUID();
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook Dell Latitude 5440", true);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));
        when(repository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new InstanciaItemCreateDTO(
                itemMestreId,
                "  NB-001  ",
                "  PAT-001  ",
                "  SN-123  ",
                null,
                "  Unidade do financeiro  ",
                true
        ));

        ArgumentCaptor<InstanciaItem> captor = ArgumentCaptor.forClass(InstanciaItem.class);
        verify(repository).save(captor.capture());

        InstanciaItem instanciaSalva = captor.getValue();
        assertThat(instanciaSalva.getItemMestre()).isEqualTo(itemMestre);
        assertThat(instanciaSalva.getIdentificador()).isEqualTo("NB-001");
        assertThat(instanciaSalva.getPatrimonio()).isEqualTo("PAT-001");
        assertThat(instanciaSalva.getNumeroSerie()).isEqualTo("SN-123");
        assertThat(instanciaSalva.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);
        assertThat(instanciaSalva.getObservacoes()).isEqualTo("Unidade do financeiro");
        assertThat(resposta.itemMestreId()).isEqualTo(itemMestreId);
        assertThat(resposta.itemMestreNome()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(resposta.statusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);
    }

    @Test
    void deveImpedirInstanciaSemIdentificacaoIndividual() {
        UUID itemMestreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.criar(new InstanciaItemCreateDTO(itemMestreId, " ", null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");

        verify(repository, never()).save(any(InstanciaItem.class));
        verify(itemMestreRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveImpedirItemMestreInativo() {
        UUID itemMestreId = UUID.randomUUID();
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook", false);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));

        assertThatThrownBy(() -> service.criar(new InstanciaItemCreateDTO(itemMestreId, "NB-001", null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Item mestre deve estar ativo");

        verify(repository, never()).save(any(InstanciaItem.class));
    }

    @Test
    void deveAtualizarInstanciaComOutroItemMestre() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        InstanciaItem instancia = instancia(id, "NB-001", itemMestre(UUID.randomUUID(), "Antigo", true));
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instancia));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));
        when(repository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new InstanciaItemUpdateDTO(
                itemMestreId,
                " NB-002 ",
                null,
                " SN-999 ",
                StatusOperacionalInstancia.EM_MOVIMENTACAO,
                null,
                false
        ));

        assertThat(resposta.itemMestreNome()).isEqualTo("Notebook novo");
        assertThat(resposta.identificador()).isEqualTo("NB-002");
        assertThat(resposta.numeroSerie()).isEqualTo("SN-999");
        assertThat(resposta.statusOperacional()).isEqualTo(StatusOperacionalInstancia.EM_MOVIMENTACAO);
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveBuscarPorIdentificadorSomenteQuandoFiltroInformado() {
        InstanciaItem instancia = instancia(UUID.randomUUID(), "NB-001", itemMestre(UUID.randomUUID(), "Notebook", true));

        when(repository.findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc("NB")).thenReturn(List.of(instancia));

        assertThat(service.buscarPorIdentificador("  ")).isEmpty();
        assertThat(service.buscarPorIdentificador(" NB ")).hasSize(1);
    }

    @Test
    void deveFiltrarPorIdentificacaoItemMestreCategoriaEStatus() {
        UUID categoriaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(UUID.randomUUID(), "NB-001", itemMestre(UUID.randomUUID(), "Notebook", true));

        when(repository.filtrarAtivas("NB", "Notebook", categoriaId, StatusOperacionalInstancia.DISPONIVEL)).thenReturn(List.of(instancia));

        var resposta = service.filtrar(" NB ", " Notebook ", categoriaId, StatusOperacionalInstancia.DISPONIVEL);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().identificador()).isEqualTo("NB-001");
        assertThat(resposta.getFirst().statusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);
    }

    private InstanciaItem instancia(UUID id, String identificador, ItemMestre itemMestre) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setId(id);
        instancia.setIdentificador(identificador);
        instancia.setItemMestre(itemMestre);
        instancia.setAtivo(true);
        return instancia;
    }

    private ItemMestre itemMestre(UUID id, String nome, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setId(UUID.randomUUID());
        categoria.setNome("Eletronicos");
        categoria.setIcone("eletronicos");

        ItemMestre itemMestre = new ItemMestre();
        itemMestre.setId(id);
        itemMestre.setNome(nome);
        itemMestre.setCategoria(categoria);
        itemMestre.setAtivo(ativo);
        return itemMestre;
    }
}
