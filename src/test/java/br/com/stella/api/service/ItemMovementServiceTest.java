package br.com.stella.api.service;

import br.com.stella.api.dto.ItemInputMovementCreateDTO;
import br.com.stella.api.dto.ItemOutputMovementCreateDTO;
import br.com.stella.api.dto.ItemTransferMovementCreateDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.ItemMovement;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.entity.ItemMovementType;
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
    private ItemMovementRepository repository;

    @Mock
    private ItemInstanceRepository instanciaItemRepository;

    @Mock
    private MainItemRepository itemMestreRepository;

    @Mock
    private StorageLocationRepository localArmazenamentoRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemMovementService service;

    @Test
    void deveRegistrarEntradaCriandoInstanciaDisponivelNoLocalDestino() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        MainItem mainItem = mainItem(itemMestreId, true);
        StorageLocation location = location(localId, "Biblioteca", true);

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location));
        when(instanciaItemRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> {
            ItemInstance instance = invocation.getArgument(0);
            instance.setId(UUID.randomUUID());
            return instance;
        });
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registerInbound(new ItemInputMovementCreateDTO(
                itemMestreId,
                localId,
                "  LIV-001  ",
                null,
                "  SN-001  ",
                "  Entrada inicial  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instance = instanciaCaptor.getValue();
        assertThat(instance.getMainItem()).isEqualTo(mainItem);
        assertThat(instance.getCurrentLocation()).isEqualTo(location);
        assertThat(instance.getIdentifier()).isEqualTo("LIV-001");
        assertThat(instance.getSerialNumber()).isEqualTo("SN-001");
        assertThat(instance.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);

        ItemMovement movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getType()).isEqualTo(ItemMovementType.ENTRADA);
        assertThat(movimentacao.getItemInstance()).isEqualTo(instance);
        assertThat(movimentacao.getDestinationLocation()).isEqualTo(location);
        assertThat(movimentacao.getOriginLocation()).isNull();
        assertThat(movimentacao.getNotes()).isEqualTo("Entrada inicial");
        assertThat(resposta.tipo()).isEqualTo(ItemMovementType.ENTRADA);
        assertThat(resposta.localDestinoId()).isEqualTo(localId);
    }

    @Test
    void deveImpedirEntradaSemIdentificacaoIndividual() {
        UUID itemMestreId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();

        when(itemMestreRepository.findById(itemMestreId)).thenReturn(Optional.of(mainItem(itemMestreId, true)));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location(localId, "Biblioteca", true)));

        assertThatThrownBy(() -> service.registerInbound(new ItemInputMovementCreateDTO(itemMestreId, localId, " ", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    @Test
    void deveRegistrarSaidaAtualizandoInstanciaELocalOrigem() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        StorageLocation location = location(localId, "Biblioteca", true);
        ItemInstance instance = instance(instanciaId, location, ItemInstanceStatus.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(instanciaItemRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registerOutbound(new ItemOutputMovementCreateDTO(
                instanciaId,
                "  Maintenance withdrawal  ",
                "  Equipment sent to the technician  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getCurrentLocation()).isNull();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.EM_MOVIMENTACAO);

        ItemMovement movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getType()).isEqualTo(ItemMovementType.SAIDA);
        assertThat(movimentacao.getItemInstance()).isEqualTo(instance);
        assertThat(movimentacao.getOriginLocation()).isEqualTo(location);
        assertThat(movimentacao.getDestinationLocation()).isNull();
        assertThat(movimentacao.getReason()).isEqualTo("Maintenance withdrawal");
        assertThat(movimentacao.getNotes()).isEqualTo("Equipment sent to the technician");
        assertThat(resposta.tipo()).isEqualTo(ItemMovementType.SAIDA);
        assertThat(resposta.localOrigemId()).isEqualTo(localId);
        assertThat(resposta.localDestinoId()).isNull();
    }

    @Test
    void deveImpedirSaidaDeInstanciaIndisponivel() {
        UUID instanciaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, location(UUID.randomUUID(), "Biblioteca", true), ItemInstanceStatus.EMPRESTADO, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.registerOutbound(new ItemOutputMovementCreateDTO(instanciaId, "Retirada", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available instances");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    @Test
    void deveRegistrarTransferenciaAtualizandoLocalAtual() {
        UUID instanciaId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        StorageLocation origem = location(origemId, "Biblioteca", true);
        StorageLocation destino = location(destinoId, "Laboratory", true);
        ItemInstance instance = instance(instanciaId, origem, ItemInstanceStatus.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(localArmazenamentoRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(instanciaItemRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movimentacao = invocation.getArgument(0);
            movimentacao.setId(UUID.randomUUID());
            return movimentacao;
        });

        var resposta = service.registerTransfer(new ItemTransferMovementCreateDTO(
                instanciaId,
                destinoId,
                "  Transfer for conference  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(instanciaItemRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getCurrentLocation()).isEqualTo(destino);
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);

        ItemMovement movimentacao = movimentacaoCaptor.getValue();
        assertThat(movimentacao.getType()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(movimentacao.getItemInstance()).isEqualTo(instance);
        assertThat(movimentacao.getOriginLocation()).isEqualTo(origem);
        assertThat(movimentacao.getDestinationLocation()).isEqualTo(destino);
        assertThat(movimentacao.getNotes()).isEqualTo("Transfer for conference");
        assertThat(resposta.tipo()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(resposta.localOrigemId()).isEqualTo(origemId);
        assertThat(resposta.localDestinoId()).isEqualTo(destinoId);
    }

    @Test
    void deveImpedirTransferenciaParaMesmoLocal() {
        UUID instanciaId = UUID.randomUUID();
        UUID localId = UUID.randomUUID();
        StorageLocation location = location(localId, "Biblioteca", true);
        ItemInstance instance = instance(instanciaId, location, ItemInstanceStatus.DISPONIVEL, true);

        when(instanciaItemRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(localArmazenamentoRepository.findById(localId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.registerTransfer(new ItemTransferMovementCreateDTO(instanciaId, localId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verify(instanciaItemRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    private MainItem mainItem(UUID id, boolean ativo) {
        MainItem mainItem = new MainItem();
        mainItem.setId(id);
        mainItem.setName("Livro");
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

    private ItemInstance instance(UUID id, StorageLocation location, ItemInstanceStatus status, boolean ativa) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setMainItem(mainItem(UUID.randomUUID(), true));
        instance.setCurrentLocation(location);
        instance.setIdentifier("LIV-001");
        instance.setOperationalStatus(status);
        instance.setActive(ativa);
        return instance;
    }
}
