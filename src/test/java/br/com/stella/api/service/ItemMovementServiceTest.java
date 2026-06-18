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
    private ItemInstanceRepository itemInstanceRepository;

    @Mock
    private MainItemRepository mainItemRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemMovementService service;

    @Test
    void shouldRegisterInboundCreatingInstanceAvailableInLocationDestination() {
        UUID mainItemId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        MainItem mainItem = mainItem(mainItemId, true);
        StorageLocation location = location(locationId, "Biblioteca", true);

        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> {
            ItemInstance instance = invocation.getArgument(0);
            instance.setId(UUID.randomUUID());
            return instance;
        });
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movement = invocation.getArgument(0);
            movement.setId(UUID.randomUUID());
            return movement;
        });

        var response = service.registerInbound(new ItemInputMovementCreateDTO(
                mainItemId,
                locationId,
                "  LIV-001  ",
                null,
                "  SN-001  ",
                "  Entrada inicial  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(itemInstanceRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instance = instanciaCaptor.getValue();
        assertThat(instance.getMainItem()).isEqualTo(mainItem);
        assertThat(instance.getCurrentLocation()).isEqualTo(location);
        assertThat(instance.getIdentifier()).isEqualTo("LIV-001");
        assertThat(instance.getSerialNumber()).isEqualTo("SN-001");
        assertThat(instance.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);

        ItemMovement movement = movimentacaoCaptor.getValue();
        assertThat(movement.getType()).isEqualTo(ItemMovementType.ENTRADA);
        assertThat(movement.getItemInstance()).isEqualTo(instance);
        assertThat(movement.getDestinationLocation()).isEqualTo(location);
        assertThat(movement.getOriginLocation()).isNull();
        assertThat(movement.getNotes()).isEqualTo("Entrada inicial");
        assertThat(response.type()).isEqualTo(ItemMovementType.ENTRADA);
        assertThat(response.destinationLocationId()).isEqualTo(locationId);
    }

    @Test
    void shouldPreventInboundWithoutIdentificationIndividual() {
        UUID mainItemId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();

        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem(mainItemId, true)));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location(locationId, "Biblioteca", true)));

        assertThatThrownBy(() -> service.registerInbound(new ItemInputMovementCreateDTO(mainItemId, locationId, " ", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    @Test
    void shouldRegisterOutboundUpdatingInstanceAndLocationOrigin() {
        UUID instanciaId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        StorageLocation location = location(locationId, "Biblioteca", true);
        ItemInstance instance = instance(instanciaId, location, ItemInstanceStatus.DISPONIVEL, true);

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movement = invocation.getArgument(0);
            movement.setId(UUID.randomUUID());
            return movement;
        });

        var response = service.registerOutbound(new ItemOutputMovementCreateDTO(
                instanciaId,
                "  Maintenance withdrawal  ",
                "  Equipment sent to the technician  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(itemInstanceRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getCurrentLocation()).isNull();
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.EM_MOVIMENTACAO);

        ItemMovement movement = movimentacaoCaptor.getValue();
        assertThat(movement.getType()).isEqualTo(ItemMovementType.SAIDA);
        assertThat(movement.getItemInstance()).isEqualTo(instance);
        assertThat(movement.getOriginLocation()).isEqualTo(location);
        assertThat(movement.getDestinationLocation()).isNull();
        assertThat(movement.getReason()).isEqualTo("Maintenance withdrawal");
        assertThat(movement.getNotes()).isEqualTo("Equipment sent to the technician");
        assertThat(response.type()).isEqualTo(ItemMovementType.SAIDA);
        assertThat(response.originLocationId()).isEqualTo(locationId);
        assertThat(response.destinationLocationId()).isNull();
    }

    @Test
    void shouldPreventOutboundOfInstanceUnavailable() {
        UUID instanciaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, location(UUID.randomUUID(), "Biblioteca", true), ItemInstanceStatus.EMPRESTADO, true);

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.registerOutbound(new ItemOutputMovementCreateDTO(instanciaId, "Retirada", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("available instances");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    @Test
    void shouldRegisterTransferUpdatingLocationCurrent() {
        UUID instanciaId = UUID.randomUUID();
        UUID origemId = UUID.randomUUID();
        UUID destinoId = UUID.randomUUID();
        StorageLocation origem = location(origemId, "Biblioteca", true);
        StorageLocation destino = location(destinoId, "Laboratory", true);
        ItemInstance instance = instance(instanciaId, origem, ItemInstanceStatus.DISPONIVEL, true);

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(storageLocationRepository.findById(destinoId)).thenReturn(Optional.of(destino));
        when(itemInstanceRepository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.save(any(ItemMovement.class))).thenAnswer(invocation -> {
            ItemMovement movement = invocation.getArgument(0);
            movement.setId(UUID.randomUUID());
            return movement;
        });

        var response = service.registerTransfer(new ItemTransferMovementCreateDTO(
                instanciaId,
                destinoId,
                "  Transfer for conference  "
        ));

        ArgumentCaptor<ItemInstance> instanciaCaptor = ArgumentCaptor.forClass(ItemInstance.class);
        ArgumentCaptor<ItemMovement> movimentacaoCaptor = ArgumentCaptor.forClass(ItemMovement.class);
        verify(itemInstanceRepository).save(instanciaCaptor.capture());
        verify(repository).save(movimentacaoCaptor.capture());

        ItemInstance instanciaAtualizada = instanciaCaptor.getValue();
        assertThat(instanciaAtualizada.getCurrentLocation()).isEqualTo(destino);
        assertThat(instanciaAtualizada.getOperationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);

        ItemMovement movement = movimentacaoCaptor.getValue();
        assertThat(movement.getType()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(movement.getItemInstance()).isEqualTo(instance);
        assertThat(movement.getOriginLocation()).isEqualTo(origem);
        assertThat(movement.getDestinationLocation()).isEqualTo(destino);
        assertThat(movement.getNotes()).isEqualTo("Transfer for conference");
        assertThat(response.type()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(response.originLocationId()).isEqualTo(origemId);
        assertThat(response.destinationLocationId()).isEqualTo(destinoId);
    }

    @Test
    void shouldPreventTransferForSameLocation() {
        UUID instanciaId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        StorageLocation location = location(locationId, "Biblioteca", true);
        ItemInstance instance = instance(instanciaId, location, ItemInstanceStatus.DISPONIVEL, true);

        when(itemInstanceRepository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.registerTransfer(new ItemTransferMovementCreateDTO(instanciaId, locationId, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        verify(itemInstanceRepository, never()).save(any(ItemInstance.class));
        verify(repository, never()).save(any(ItemMovement.class));
    }

    private MainItem mainItem(UUID id, boolean active) {
        MainItem mainItem = new MainItem();
        mainItem.setId(id);
        mainItem.setName("Livro");
        mainItem.setActive(active);
        return mainItem;
    }

    private StorageLocation location(UUID id, String name, boolean active) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(name);
        location.setActive(active);
        return location;
    }

    private ItemInstance instance(UUID id, StorageLocation location, ItemInstanceStatus status, boolean active) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setMainItem(mainItem(UUID.randomUUID(), true));
        instance.setCurrentLocation(location);
        instance.setIdentifier("LIV-001");
        instance.setOperationalStatus(status);
        instance.setActive(active);
        return instance;
    }
}
