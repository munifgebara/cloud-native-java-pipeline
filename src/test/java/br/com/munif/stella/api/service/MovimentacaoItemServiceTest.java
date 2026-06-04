package br.com.munif.stella.api.service;

import br.com.munif.stella.api.dto.MovimentacaoEntradaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoSaidaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoTransferenciaCreateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovimentacaoItemServiceTest {

    @Mock
    private MovimentacaoItemRepository repository;

    @Mock
    private InstanciaItemRepository instanciaItemRepository;

    @Mock
    private ItemMestreRepository itemMestreRepository;

    @Mock
    private LocalArmazenamentoRepository localArmazenamentoRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MovimentacaoItemService service;

    @Test
    void deveRegistrarEntradaCriandoInstanciaDisponivelNoLocalDestino() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        ItemMestre itemMestre = itemMestre(itemMestreId, true);
        LocalArmazenamento local = local(localId, "Biblioteca", true);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(local));
        when(instanciaItemRepository.save(any(InstanciaItem.class))).thenAnswer(invocation -> {
            InstanciaItem instancia = invocation.getArgument(0);
            instancia.setId(UUID.randomUUID());
            return instancia;
        });
        when(repository.save(any(MovimentacaoItem.class))).thenAnswer(invocation -> {
            MovimentacaoItem movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registrarEntrada(new MovimentacaoEntradaCreateDTO(
                itemMestreId,
                localId,
                "  LIV-001  ",
                null,
                "  SN-001  ",
                "  Entrada inicial  "
        ));

        ArgumentCaptor<InstanciaItem> instanciaCaptor = ArgumentCaptor.forClass(InstanciaItem.class);
        ArgumentCaptor<MovimentacaoItem> movimentacaoCaptor = ArgumentCaptor.forClass(MovimentacaoItem.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        InstanciaItem instancia = instanciaCaptor.getValue();
        assertThat(instancia.getItemMestre()).isEqualTo(itemMestre);
        assertThat(instancia.getLocalAtual()).isEqualTo(local);
        assertThat(instancia.getIdentificador()).isEqualTo("LIV-001");
        assertThat(instancia.getNumeroSerie()).isEqualTo("SN-001");
        assertThat(instancia.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);

        MovimentacaoItem movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacaoItem.ENTRADA);
        assertThat(movimentacao.getInstanciaItem()).isEqualTo(instancia);
        assertThat(movimentacao.getLocalDestino()).isEqualTo(local);
        assertThat(movimentacao.getLocalOrigem()).isNull();
        assertThat(movimentacao.getObservacao()).isEqualTo("Entrada inicial");
        assertThat(resposta.tipo()).isEqualTo(TipoMovimentacaoItem.ENTRADA);
        assertThat(resposta.localDestinoId()).isEqualTo(localId);
    }

    @Test
    void deveImpedirEntradaSemIdentificacaoIndividual() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(itemMestre(itemMestreId, true)));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(local(localId, "Biblioteca", true)));

        assertThatThrownBy(() -> service.registrarEntrada(new MovimentacaoEntradaCreateDTO(itemMestreId, localId, " ", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identificador");

        verify(instanciaItemRepository, never()).save(any(InstanciaItem.class));
        verify(repository, never()).save(any(MovimentacaoItem.class));
    }

    @Test
    void deveRegistrarSaidaAtualizandoInstanciaELocalOrigem() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        LocalArmazenamento local = local(localId, "Biblioteca", true);
        InstanciaItem instancia = instancia(instanciaId, local, StatusOperacionalInstancia.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(instanciaItemRepository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(MovimentacaoItem.class))).thenAnswer(invocation -> {
            MovimentacaoItem movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registrarSaida(new MovimentacaoSaidaCreateDTO(
                instanciaId,
                "  Retirada para manutenção  ",
                "  Equipamento enviado ao técnico  "
        ));

        ArgumentCaptor<InstanciaItem> instanciaCaptor = ArgumentCaptor.forClass(InstanciaItem.class);
        ArgumentCaptor<MovimentacaoItem> movimentacaoCaptor = ArgumentCaptor.forClass(MovimentacaoItem.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        InstanciaItem instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getLocalAtual()).isNull();
        assertThat(instanciaAtualizada.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.EM_MOVIMENTACAO);

        MovimentacaoItem movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacaoItem.SAIDA);
        assertThat(movimentacao.getInstanciaItem()).isEqualTo(instancia);
        assertThat(movimentacao.getLocalOrigem()).isEqualTo(local);
        assertThat(movimentacao.getLocalDestino()).isNull();
        assertThat(movimentacao.getMotivo()).isEqualTo("Retirada para manutenção");
        assertThat(movimentacao.getObservacao()).isEqualTo("Equipamento enviado ao técnico");
        assertThat(resposta.tipo()).isEqualTo(TipoMovimentacaoItem.SAIDA);
        assertThat(resposta.localOrigemId()).isEqualTo(localId);
        assertThat(resposta.localDestinoId()).isNull();
    }

    @Test
    void deveImpedirSaidaDeInstanciaIndisponivel() {
        UUID instanciaId = UUID.randomUUID();
        InstanciaItem instancia = instancia(instanciaId, local(UUID.randomUUID(), "Biblioteca", true), StatusOperacionalInstancia.EMPRESTADO, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));

        assertThatThrownBy(() -> service.registrarSaida(new MovimentacaoSaidaCreateDTO(instanciaId, "Retirada", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disponíveis");

        verify(instanciaItemRepository, never()).save(any(InstanciaItem.class));
        verify(repository, never()).save(any(MovimentacaoItem.class));
    }

    @Test
    void deveRegistrarTransferenciaAtualizandoLocalAtual() {
        UUID instanciaId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        LocalArmazenamento origem = local(origemId, "Biblioteca", true);
        LocalArmazenamento destino = local(destinoId, "Laboratório", true);
        InstanciaItem instancia = instancia(instanciaId, origem, StatusOperacionalInstancia.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(localArmazenamentoRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(instanciaItemRepository.save(any(InstanciaItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(MovimentacaoItem.class))).thenAnswer(invocation -> {
            MovimentacaoItem movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registrarTransferencia(new MovimentacaoTransferenciaCreateDTO(
                instanciaId,
                destinoId,
                "  Transferência para conferência  "
        ));

        ArgumentCaptor<InstanciaItem> instanciaCaptor = ArgumentCaptor.forClass(InstanciaItem.class);
        ArgumentCaptor<MovimentacaoItem> movimentacaoCaptor = ArgumentCaptor.forClass(MovimentacaoItem.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        InstanciaItem instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getLocalAtual()).isEqualTo(destino);
        assertThat(instanciaAtualizada.getStatusOperacional()).isEqualTo(StatusOperacionalInstancia.DISPONIVEL);

        MovimentacaoItem movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getTipo()).isEqualTo(TipoMovimentacaoItem.TRANSFERENCIA);
        assertThat(movimentacao.getInstanciaItem()).isEqualTo(instancia);
        assertThat(movimentacao.getLocalOrigem()).isEqualTo(origem);
        assertThat(movimentacao.getLocalDestino()).isEqualTo(destino);
        assertThat(movimentacao.getObservacao()).isEqualTo("Transferência para conferência");
        assertThat(resposta.tipo()).isEqualTo(TipoMovimentacaoItem.TRANSFERENCIA);
        assertThat(resposta.localOrigemId()).isEqualTo(origemId);
        assertThat(resposta.localDestinoId()).isEqualTo(destinoId);
    }

    @Test
    void deveImpedirTransferenciaParaMesmoLocal() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        LocalArmazenamento local = local(localId, "Biblioteca", true);
        InstanciaItem instancia = instancia(instanciaId, local, StatusOperacionalInstancia.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instancia));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(local));

        assertThatThrownBy(() -> service.registrarTransferencia(new MovimentacaoTransferenciaCreateDTO(instanciaId, localId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diferente");

        verify(instanciaItemRepository, never()).save(any(InstanciaItem.class));
        verify(repository, never()).save(any(MovimentacaoItem.class));
    }

    private ItemMestre itemMestre(UUID id, boolean ativo) {
        ItemMestre itemMestre = new ItemMestre();
        itemMestre.setId(id);
        itemMestre.setNome("Livro");
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

    private InstanciaItem instancia(UUID id, LocalArmazenamento local, StatusOperacionalInstancia status, boolean ativa) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setId(id);
        instancia.setItemMestre(itemMestre(UUID.randomUUID(), true));
        instancia.setLocalAtual(local);
        instancia.setIdentificador("LIV-001");
        instancia.setStatusOperacional(status);
        instancia.setAtivo(ativa);
        return instancia;
    }
}
