package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
import br.com.munif.stella.api.repository.EmprestimoItemRepository;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.MovimentacaoItemRepository;
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
    private InstanciaItemRepository repository;

    @Mock
    private ItemMestreRepository itemMestreRepository;

    @Mock
    private LocalArmazenamentoRepository localArmazenamentoRepository;

    @Mock
    private MovimentacaoItemRepository movimentacaoItemRepository;

    @Mock
    private EmprestimoItemRepository emprestimoItemRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private InstanciaItemService service;

    @Test
    void deveCriarInstanciaComItemMestreNormalizandoCampos() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook Dell Latitude 5440", true);
        LocalArmazenamento local = local(localId, "Estante A", true);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(local));
        when(repository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var resposta = service.criar(new InstanciaItemCreateDTO(
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

        ArgumentCaptor<InstanciaItem> captor = ArgumentCaptor.forClass(InstanciaItem.class);
        verify(repository).save(captor.capture());

        InstanciaItem instanciaSalva = captor.getValue();
        assertThat(instanciaSalva.getItemMestre()).isEqualTo(itemMestre);
        assertThat(instanciaSalva.getLocalAtual()).isEqualTo(local);
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

        assertThatThrownBy(() -> service.criar(new InstanciaItemCreateDTO(itemMestreId, null, " ", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");

        verify(repository, never()).save(any(InstanciaItem.class));
        verify(itemMestreRepository, never()).findById(any(UUID.class));
    }

    @Test
    void deveImpedirItemMestreInativo() {
        UUID itemMestreId = UUID.randomUUID();
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook", false);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));

        assertThatThrownBy(() -> service.criar(new InstanciaItemCreateDTO(itemMestreId, null, "NB-001", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Main item must be active");

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
                null,
                " NB-002 ",
                null,
                " SN-999 ",
                StatusOperacionalInstancia.EM_MOVIMENTACAO,
                null,
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
    void deveImpedirInstanciaDisponivelSemLocalAtual() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        InstanciaItem instancia = instancia(id, "NB-001", itemMestre(UUID.randomUUID(), "Antigo", true));
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instancia));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));

        assertThatThrownBy(() -> service.atualizar(id, new InstanciaItemUpdateDTO(
                itemMestreId,
                null,
                "NB-001",
                null,
                null,
                StatusOperacionalInstancia.DISPONIVEL,
                null,
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Available instance must have a current location");

        verify(repository, never()).save(any(InstanciaItem.class));
    }

    @Test
    void deveImpedirInstanciaEmprestadaComLocalAtual() {
        UUID id = UUID.randomUUID();
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        InstanciaItem instancia = instancia(id, "NB-001", itemMestre(UUID.randomUUID(), "Antigo", true));
        ItemMestre itemMestre = itemMestre(itemMestreId, "Notebook novo", true);
        LocalArmazenamento local = local(localId, "Biblioteca", true);

        when(repository.findById(id)).thenReturn(Optional.of(instancia));
        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(local));

        assertThatThrownBy(() -> service.atualizar(id, new InstanciaItemUpdateDTO(
                itemMestreId,
                localId,
                "NB-001",
                null,
                null,
                StatusOperacionalInstancia.EMPRESTADO,
                null,
                null,
                true
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Loaned instance must not have a current location");

        verify(repository, never()).save(any(InstanciaItem.class));
    }

    @Test
    void deveImpedirExclusaoLogicaDeInstanciaComMovimentacao() {
        UUID id = UUID.randomUUID();

        when(movimentacaoItemRepository.existsByInstanciaItemId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirLogicamente(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(InstanciaItem.class));
    }

    @Test
    void deveImpedirExclusaoLogicaDeInstanciaComEmprestimo() {
        UUID id = UUID.randomUUID();

        when(movimentacaoItemRepository.existsByInstanciaItemId(id)).thenReturn(false);
        when(emprestimoItemRepository.existsByInstanciaItemId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.excluirLogicamente(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(InstanciaItem.class));
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

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(instancia));

        var resposta = service.filtrar(" NB ", " Notebook ", categoriaId, StatusOperacionalInstancia.DISPONIVEL);

        assertThat(resposta).hasSize(1);
        assertThat(resposta.getFirst().identificador()).isEqualTo("NB-001");
        assertThat(resposta.getFirst().statusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);
    }

    @Test
    void deveBuscarHistoricoConsolidadoComMovimentacoesOrdenadas() {
        UUID instanciaId = UUID.randomUUID();
        LocalArmazenamento origem = local(UUID.randomUUID(), "Biblioteca", true);
        LocalArmazenamento destino = local(UUID.randomUUID(), "Laboratorio", true);
        InstanciaItem instancia = instancia(instanciaId, "NB-001", itemMestre(UUID.randomUUID(), "Notebook", true));
        instancia.setLocalAtual(destino);
        MovimentacaoItem movimentacao = movimentacao(instancia, origem, destino, TipoMovimentacaoItem.TRANSFERENCIA);

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(instanciaId))
                .thenReturn(List.of(movimentacao));

        var resposta = service.buscarHistorico(instanciaId);

        assertThat(resposta.instancia().id()).isEqualTo(instanciaId);
        assertThat(resposta.instancia().localAtualId()).isEqualTo(destino.getId());
        assertThat(resposta.movimentacoes()).hasSize(1);
        assertThat(resposta.movimentacoes().getFirst().tipo()).isEqualTo(TipoMovimentacaoItem.TRANSFERENCIA);
        assertThat(resposta.movimentacoes().getFirst().localOrigemId()).isEqualTo(origem.getId());
        assertThat(resposta.movimentacoes().getFirst().localDestinoId()).isEqualTo(destino.getId());
    }

    @Test
    void deveBuscarHistoricoSemMovimentacoes() {
        UUID instanciaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(instanciaId, "NB-001", itemMestre(UUID.randomUUID(), "Notebook", true));

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(instanciaId))
                .thenReturn(List.of());

        var resposta = service.buscarHistorico(instanciaId);

        assertThat(resposta.instancia().id()).isEqualTo(instanciaId);
        assertThat(resposta.movimentacoes()).isEmpty();
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

    private LocalArmazenamento local(UUID id, String nome, boolean ativo) {
        LocalArmazenamento local = new LocalArmazenamento();
        local.setId(id);
        local.setNome(nome);
        local.setAtivo(ativo);
        return local;
    }

    private MovimentacaoItem movimentacao(InstanciaItem instancia, LocalArmazenamento origem, LocalArmazenamento destino, TipoMovimentacaoItem tipo) {
        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setId(UUID.randomUUID());
        movimentacao.setTipo(tipo);
        movimentacao.setInstanciaItem(instancia);
        movimentacao.setLocalOrigem(origem);
        movimentacao.setLocalDestino(destino);
        movimentacao.setObservacao("Movimento registrado");
        return movimentacao;
    }
}
