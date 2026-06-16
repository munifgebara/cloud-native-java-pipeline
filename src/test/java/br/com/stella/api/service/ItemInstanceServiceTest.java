package br.com.stella.api.service;

import br.com.stella.api.dto.ItemInstanceCreateDTO;
import br.com.stella.api.dto.ItemInstanceUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemMovementType;
import br.com.stella.api.repository.ItemLoanRepository;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import br.com.stella.api.repository.ItemMovementRepository;
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

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstanciaItemServiceTest {

    @Mock
    private ItemInstanceRepository repository;

    @Mock
    private MainItemRepository itemMestreRepository;

    @Mock
    private StorageLocationRepository localArmazenamentoRepository;

    @Mock
    private ItemMovementRepository movimentacaoItemRepository;

    @Mock
    private ItemLoanRepository emprestimoItemRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemInstanceService service;

    @Test
    void deveCriarInstanciaComItemMestreNormalizandoCampos() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        MainItem mainItem = mainItem(itemMestreId, "Notebook Dell Latitude 5440", true);
        StorageLocation location = location(localId, "Estante A", true);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location));
        when(repository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new ItemInstanceCreateDTO(
                itemMestreId,
                localId,
                "  NB-001  ",
                "  PAT-001  ",
                "  SN-123  ",
                null,
                "  Unidade do financeiro  ",
                null,
                true
        ));

        ArgumentCaptor<ItemInstance> captor = ArgumentCaptor.forClass(ItemInstance.class);
        verify(repository).save(captor.capture());

        ItemInstance instanciaSalva = captor.getValue();
        assertThat(instanciaSalva.getMainItem()).isEqualTo(mainItem);
        assertThat(instanciaSalva.getCurrentLocation()).isEqualTo(location);
        assertThat(instanciaSalva.getIdentifier()).isEqualTo("NB-001");
        assertThat(instanciaSalva.getAssetTag()).isEqualTo("PAT-001");
        assertThat(instanciaSalva.getSerialNumber()).isEqualTo("SN-123");
        assertThat(instanciaSalva.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
        assertThat(instanciaSalva.getNotes()).isEqualTo("Unidade do financeiro");
        assertThat(resposta.itemMestreId()).isEqualTo(itemMestreId);
        assertThat(resposta.itemMestreNome()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(resposta.statusOperacional()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
    }

    @Test
    void deveImpedirInstanciaSemIdentificacaoIndividual() {
        UUID itemMestreId = UUID.randomUUID();

        assertThatThrownBy(() -> service.criar(new ItemInstanceCreateDTO(itemMestreId, null, " ", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");

        verify(repository, never()).save(any(ItemInstance.class));
        verify(itemMestreRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveImpedirItemMestreInativo() {
        UUID itemMestreId = UUID.randomUUID();
        MainItem mainItem = mainItem(itemMestreId, "Notebook", false);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));

        assertThatThrownBy(() -> service.criar(new ItemInstanceCreateDTO(itemMestreId, null, "NB-001", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Main item must be active");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void deveAtualizarInstanciaComOutroItemMestre() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(itemMestreId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));
        when(repository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.atualizar(id, new ItemInstanceUpdateDTO(
                itemMestreId,
                null,
                " NB-002 ",
                null,
                " SN-999 ",
                ItemInstanceStatus.EM_MOVIMENTACAO,
                null,
                null,
                false
        ));

        assertThat(resposta.itemMestreNome()).isEqualTo("Notebook novo");
        assertThat(resposta.identificador()).isEqualTo("NB-002");
        assertThat(resposta.numeroSerie()).isEqualTo("SN-999");
        assertThat(resposta.statusOperacional()).isEqualTo(ItemInstanceStatus.EM_MOVIMENTACAO);
        assertThat(resposta.ativa()).isFalse();
    }

    @Test
    void deveImpedirInstanciaDisponivelSemLocalAtual() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(itemMestreId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));

        assertThatThrownBy(() -> service.atualizar(id, new ItemInstanceUpdateDTO(
                itemMestreId,
                null,
                "NB-001",
                null,
                null,
                ItemInstanceStatus.DISPONIVEL,
                null,
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available instance must have a current location");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void deveImpedirInstanciaEmprestadaComLocalAtual() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(itemMestreId, "Notebook novo", true);
        StorageLocation location = location(localId, "Biblioteca", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.atualizar(id, new ItemInstanceUpdateDTO(
                itemMestreId,
                localId,
                "NB-001",
                null,
                null,
                ItemInstanceStatus.EMPRESTADO,
                null,
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loaned instance must not have a current location");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void deveImpedirExclusaoLogicaDeInstanciaComMovimentacao() {
        UUID id = UUID.randomUUID();

        when(movimentacaoItemRepository.existsByInstanciaItemId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirLogicamente(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void deveImpedirExclusaoLogicaDeInstanciaComEmprestimo() {
        UUID id = UUID.randomUUID();

        when(movimentacaoItemRepository.existsByInstanciaItemId(id)).thenReturn(false);
        when(emprestimoItemRepository.existsByInstanciaItemId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirLogicamente(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void deveBuscarPorIdentificadorSomenteQuandoFiltroInformado() {
        ItemInstance instance = instance(UUID.randomUUID(), "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc("NB")).thenReturn(List.of(instance));

        assertThat(service.buscarPorIdentificador("  ")).isEmpty();
        assertThat(service.buscarPorIdentificador(" NB ")).hasSize(1);
    }

    @Test
    void deveFiltrarPorIdentificacaoItemMestreCategoriaEStatus() {
        UUID categoriaId = UUID.randomUUID();
        ItemInstance instance = instance(UUID.randomUUID(), "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(instance));

        var resposta = service.filtrar(" NB ", " Notebook ", categoriaId, ItemInstanceStatus.DISPONIVEL);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().identificador()).isEqualTo("NB-001");
        assertThat(resposta.getFirst().statusOperacional()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
    }

    @Test
    void deveBuscarHistoricoConsolidadoComMovimentacoesOrdenadas() {
        UUID instanciaId = UUID.randomUUID();
        StorageLocation origem = location(UUID.randomUUID(), "Biblioteca", true);
        StorageLocation destino = location(UUID.randomUUID(), "Laboratorio", true);
        ItemInstance instance = instance(instanciaId, "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));
        instance.setCurrentLocation(destino);
        ItemMovement movimentacao = movimentacao(instance, origem, destino, ItemMovementType.TRANSFERENCIA);

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(instanciaId))
                .thenReturn(List.of(movimentacao));

        var resposta = service.buscarHistorico(instanciaId);

        assertThat(resposta.instance().id()).isEqualTo(instanciaId);
        assertThat(resposta.instance().localAtualId()).isEqualTo(destino.getId());
        assertThat(resposta.movimentacoes()).hasSize(1);
        assertThat(resposta.movimentacoes().getFirst().tipo()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(resposta.movimentacoes().getFirst().localOrigemId()).isEqualTo(origem.getId());
        assertThat(resposta.movimentacoes().getFirst().localDestinoId()).isEqualTo(destino.getId());
    }

    @Test
    void deveBuscarHistoricoSemMovimentacoes() {
        UUID instanciaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(instanciaId))
                .thenReturn(List.of());

        var resposta = service.buscarHistorico(instanciaId);

        assertThat(resposta.instance().id()).isEqualTo(instanciaId);
        assertThat(resposta.movimentacoes()).isEmpty();
    }

    private ItemInstance instance(UUID id, String identificador, MainItem mainItem) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setIdentifier(identificador);
        instance.setMainItem(mainItem);
        instance.setActive(true);
        return instance;
    }

    private MainItem mainItem(UUID id, String nome, boolean ativo) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Eletronicos");
        category.setIcon("eletronicos");

        MainItem mainItem = new MainItem();
        mainItem.setId(id);
        mainItem.setName(nome);
        mainItem.setCategory(category);
        mainItem.setActive(ativo);
        return mainItem;
    }

    private StorageLocation location(UUID id, String nome, boolean ativo) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(nome);
        location.setActive(ativo);
        return location;
    }

    private ItemMovement movimentacao(ItemInstance instance, StorageLocation origem, StorageLocation destino, ItemMovementType tipo) {
        ItemMovement movimentacao = new ItemMovement();
        movimentacao.setId(UUID.randomUUID());
        movimentacao.setType(tipo);
        movimentacao.setItemInstance(instance);
        movimentacao.setOriginLocation(origem);
        movimentacao.setDestinationLocation(destino);
        movimentacao.setNotes("Movimento registrado");
        return movimentacao;
    }
}
