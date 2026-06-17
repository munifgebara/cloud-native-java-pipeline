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
    private MainItemRepository mainItemRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private ItemMovementRepository itemMovementRepository;

    @Mock
    private ItemLoanRepository itemLoanRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ItemInstanceService service;

    @Test
    void shouldCreateInstanceWithItemMainNormalizingFields() {
        UUID mainItemId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        MainItem mainItem = mainItem(mainItemId, "Notebook Dell Latitude 5440", true);
        StorageLocation location = location(locationId, "Estante A", true);

        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location));
        when(repository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new ItemInstanceCreateDTO(
                mainItemId,
                locationId,
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
        assertThat(response.mainItemId()).isEqualTo(mainItemId);
        assertThat(response.mainItemName()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(response.operationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
    }

    @Test
    void shouldPreventInstanceWithoutIdentificationIndividual() {
        UUID mainItemId = UUID.randomUUID();

        assertThatThrownBy(() -> service.create(new ItemInstanceCreateDTO(mainItemId, null, " ", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identifier");

        verify(repository, never()).save(any(ItemInstance.class));
        verify(mainItemRepository, never()).findById(any(UUID.class));
    }

    @Test
    void shouldPreventItemMainInactive() {
        UUID mainItemId = UUID.randomUUID();
        MainItem mainItem = mainItem(mainItemId, "Notebook", false);

        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));

        assertThatThrownBy(() -> service.create(new ItemInstanceCreateDTO(mainItemId, null, "NB-001", null, null, null, null, null, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Main item must be active");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void shouldUpdateInstanceWithAnotherItemMain() {
        UUID id = UUID.randomUUID();
        UUID mainItemId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(mainItemId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));
        when(repository.save(any(ItemInstance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, new ItemInstanceUpdateDTO(
                mainItemId,
                null,
                " NB-002 ",
                null,
                " SN-999 ",
                ItemInstanceStatus.EM_MOVIMENTACAO,
                null,
                null,
                false
        ));

        assertThat(response.mainItemName()).isEqualTo("Notebook novo");
        assertThat(response.identifier()).isEqualTo("NB-002");
        assertThat(response.serialNumber()).isEqualTo("SN-999");
        assertThat(response.operationalStatus()).isEqualTo(ItemInstanceStatus.EM_MOVIMENTACAO);
        assertThat(response.active()).isFalse();
    }

    @Test
    void shouldPreventInstanceAvailableWithoutLocationCurrent() {
        UUID id = UUID.randomUUID();
        UUID mainItemId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(mainItemId, "Notebook novo", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));

        assertThatThrownBy(() -> service.update(id, new ItemInstanceUpdateDTO(
                mainItemId,
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
    void shouldPreventInstanceLoanedWithLocationCurrent() {
        UUID id = UUID.randomUUID();
        UUID mainItemId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        ItemInstance instance = instance(id, "NB-001", mainItem(UUID.randomUUID(), "Antigo", true));
        MainItem mainItem = mainItem(mainItemId, "Notebook novo", true);
        StorageLocation location = location(locationId, "Biblioteca", true);

        when(repository.findById(id)).thenReturn(Optional.of(instance));
        when(mainItemRepository.findById(mainItemId)).thenReturn(Optional.of(mainItem));
        when(storageLocationRepository.findById(locationId)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.update(id, new ItemInstanceUpdateDTO(
                mainItemId,
                locationId,
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
    void shouldPreventDeletionLogicOfInstanceWithMovement() {
        UUID id = UUID.randomUUID();

        when(itemMovementRepository.existsByItemInstanceId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteLogically(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void shouldPreventDeletionLogicOfInstanceWithLoan() {
        UUID id = UUID.randomUUID();

        when(itemMovementRepository.existsByItemInstanceId(id)).thenReturn(false);
        when(itemLoanRepository.existsByItemInstanceId(id)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteLogically(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("operational history");

        verify(repository, never()).save(any(ItemInstance.class));
    }

    @Test
    void shouldFindByIdentifierOnlyWhenFilterProvided() {
        ItemInstance instance = instance(UUID.randomUUID(), "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findByActiveTrueAndIdentifierContainingIgnoreCaseOrderByIdentifierAsc("NB")).thenReturn(List.of(instance));

        assertThat(service.findByIdentifier("  ")).isEmpty();
        assertThat(service.findByIdentifier(" NB ")).hasSize(1);
    }

    @Test
    void shouldFilterByIdentificationItemMainCategoryAndStatus() {
        UUID categoryId = UUID.randomUUID();
        ItemInstance instance = instance(UUID.randomUUID(), "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(instance));

        var response = service.filter(" NB ", " Notebook ", categoryId, ItemInstanceStatus.DISPONIVEL);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().identifier()).isEqualTo("NB-001");
        assertThat(response.getFirst().operationalStatus()).isEqualTo(ItemInstanceStatus.DISPONIVEL);
    }

    @Test
    void shouldFindHistoryConsolidatedWithMovementsOrdered() {
        UUID instanciaId = UUID.randomUUID();
        StorageLocation origem = location(UUID.randomUUID(), "Biblioteca", true);
        StorageLocation destino = location(UUID.randomUUID(), "Laboratorio", true);
        ItemInstance instance = instance(instanciaId, "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));
        instance.setCurrentLocation(destino);
        ItemMovement movement = movement(instance, origem, destino, ItemMovementType.TRANSFERENCIA);

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(itemMovementRepository.findByItemInstanceIdOrderByMovementDateAscCreatedAtAsc(instanciaId))
                .thenReturn(List.of(movement));

        var response = service.findHistory(instanciaId);

        assertThat(response.instance().id()).isEqualTo(instanciaId);
        assertThat(response.instance().currentLocationId()).isEqualTo(destino.getId());
        assertThat(response.movimentacoes()).hasSize(1);
        assertThat(response.movimentacoes().getFirst().tipo()).isEqualTo(ItemMovementType.TRANSFERENCIA);
        assertThat(response.movimentacoes().getFirst().originLocationId()).isEqualTo(origem.getId());
        assertThat(response.movimentacoes().getFirst().destinationLocationId()).isEqualTo(destino.getId());
    }

    @Test
    void shouldFindHistoryWithoutMovements() {
        UUID instanciaId = UUID.randomUUID();
        ItemInstance instance = instance(instanciaId, "NB-001", mainItem(UUID.randomUUID(), "Notebook", true));

        when(repository.findById(instanciaId)).thenReturn(Optional.of(instance));
        when(itemMovementRepository.findByItemInstanceIdOrderByMovementDateAscCreatedAtAsc(instanciaId))
                .thenReturn(List.of());

        var response = service.findHistory(instanciaId);

        assertThat(response.instance().id()).isEqualTo(instanciaId);
        assertThat(response.movimentacoes()).isEmpty();
    }

    private ItemInstance instance(UUID id, String identifier, MainItem mainItem) {
        ItemInstance instance = new ItemInstance();
        instance.setId(id);
        instance.setIdentifier(identifier);
        instance.setMainItem(mainItem);
        instance.setActive(true);
        return instance;
    }

    private MainItem mainItem(UUID id, String name, boolean active) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Eletronicos");
        category.setIcon("eletronicos");

        MainItem mainItem = new MainItem();
        mainItem.setId(id);
        mainItem.setName(name);
        mainItem.setCategory(category);
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

    private ItemMovement movement(ItemInstance instance, StorageLocation origem, StorageLocation destino, ItemMovementType tipo) {
        ItemMovement movement = new ItemMovement();
        movement.setId(UUID.randomUUID());
        movement.setType(tipo);
        movement.setItemInstance(instance);
        movement.setOriginLocation(origem);
        movement.setDestinationLocation(destino);
        movement.setNotes("Movimento registrado");
        return movement;
    }
}
