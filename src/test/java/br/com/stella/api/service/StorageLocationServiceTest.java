package br.com.stella.api.service;

import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.repository.StorageLocationRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class LocalArmazenamentoServiceTest {

    @Mock
    private StorageLocationRepository repository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private MainItemImageStorageService imageStorageService;

    @InjectMocks
    private StorageLocationService service;

    @Test
    void shouldCreateLocationRootNormalizingFields() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new StorageLocationCreateDTO("  Casa  ", "  Local principal  ", null, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        StorageLocation localSalvo = captor.getValue();
        assertThat(localSalvo.getName()).isEqualTo("Casa");
        assertThat(localSalvo.getDescription()).isEqualTo("Local principal");
        assertThat(localSalvo.getParent()).isNull();
        assertThat(response.name()).isEqualTo("Casa");
    }

    @Test
    void shouldCreateSubLocationWithLocationParent() {
        UUID parentId = UUID.randomUUID();
        StorageLocation parent = location(parentId, "Escritorio", null);

        when(repository.findById(parentId)).thenReturn(Optional.of(parent));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(new StorageLocationCreateDTO("Armario 1", null, parentId, true));

        ArgumentCaptor<StorageLocation> captor = ArgumentCaptor.forClass(StorageLocation.class);
        verify(repository).save(captor.capture());

        assertThat(captor.getValue().getParent()).isEqualTo(parent);
    }

    @Test
    void shouldCreateLocationInactiveAfterPersistenceInitial() {
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new StorageLocationCreateDTO("Arquivo morto", null, null, false));

        assertThat(response.active()).isFalse();
        verify(repository).flush();
    }

    @Test
    void shouldListPreservingHierarchy() {
        StorageLocation casa = location(UUID.randomUUID(), "Casa", null);
        StorageLocation escritorio = location(UUID.randomUUID(), "Escritorio", casa);
        StorageLocation gaveta = location(UUID.randomUUID(), "Gaveta 2", escritorio);
        StorageLocation deposito = location(UUID.randomUUID(), "Deposito", null);

        when(repository.findAllActive(any(Sort.class))).thenReturn(List.of(deposito, gaveta, escritorio, casa));

        var locais = service.listSummary();

        assertThat(locais).extracting("name").containsExactly("Casa", "Escritorio", "Gaveta 2", "Deposito");
        assertThat(locais).extracting("path").containsExactly(
                "Casa",
                "Casa > Escritorio",
                "Casa > Escritorio > Gaveta 2",
                "Deposito"
        );
        assertThat(locais).extracting("level").containsExactly(0, 1, 2, 0);
    }

    @Test
    void shouldListResumoIncludingInactiveAndOrphansAsRoot() {
        StorageLocation removido = location(UUID.randomUUID(), "Removido", null);
        removido.setActive(false);
        StorageLocation orfao = location(UUID.randomUUID(), "Orfao", location(UUID.randomUUID(), "Pai ausente", null));

        when(repository.findAllIncludingInactive()).thenReturn(List.of(orfao, removido));

        var locais = service.listSummaryIncludingInactive();

        assertThat(locais).extracting("name").containsExactly("Orfao", "Removido");
        assertThat(locais).extracting("level").containsExactly(0, 0);
        assertThat(locais.getLast().active()).isFalse();
    }

    @Test
    void shouldFindByNameOnlyWhenFilterProvided() {
        StorageLocation location = location(UUID.randomUUID(), "Deposito Central", null);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(location));

        assertThat(service.findByName("  ")).isEmpty();
        assertThat(service.findByName(" Deposito ")).hasSize(1);
    }

    @Test
    void shouldPreventLocationAsParentItsSame() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Casa", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.update(id, new StorageLocationUpdateDTO("Casa", null, id, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void shouldPreventParentDescendingOfOwnLocation() {
        UUID casaId = UUID.randomUUID();
        UUID escritorioId = UUID.randomUUID();
        UUID gavetaId = UUID.randomUUID();

        StorageLocation casa = location(casaId, "Casa", null);
        StorageLocation escritorio = location(escritorioId, "Escritorio", casa);
        StorageLocation gaveta = location(gavetaId, "Gaveta", escritorio);

        when(repository.findById(casaId)).thenReturn(Optional.of(casa));
        when(repository.findById(gavetaId)).thenReturn(Optional.of(gaveta));

        assertThatThrownBy(() -> service.update(casaId, new StorageLocationUpdateDTO("Casa", null, gavetaId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descendant");
    }

    @Test
    void shouldPreventLocationParentInactive() {
        UUID parentId = UUID.randomUUID();
        StorageLocation parent = location(parentId, "Arquivo morto", null);
        parent.setActive(false);

        when(repository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThatThrownBy(() -> service.create(new StorageLocationCreateDTO("Caixa", null, parentId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Parent location must be active.");

        verify(repository, never()).save(any(StorageLocation.class));
    }

    @Test
    void shouldUpdateLocationWithParentActiveNormalizingFields() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        StorageLocation location = location(id, "Antigo", null);
        StorageLocation parent = location(parentId, "Sala", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.findById(parentId)).thenReturn(Optional.of(parent));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, new StorageLocationUpdateDTO("  Armario  ", "  Documentos  ", parentId, false));

        assertThat(response.name()).isEqualTo("Armario");
        assertThat(response.description()).isEqualTo("Documentos");
        assertThat(response.parentId()).isEqualTo(parentId);
        assertThat(response.active()).isFalse();
    }

    @Test
    void shouldUpdateImageOfLocationRemovingImagePrevious() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("bucket-antigo");
        location.setImageObjectKey("locais/%s/antiga.png".formatted(id));

        var file = new org.springframework.mock.web.MockMultipartFile("file", "nova.png", "image/png", new byte[]{1, 2});
        var image = new MainItemImageDTO("bucket-novo", "locais/%s/nova.png".formatted(id), "image/png", 2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imageStorageService.storeLocationImage(id, file)).thenReturn(image);
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateImage(id, file);

        assertThat(response.imageUrl()).isEqualTo("/api/public/locations/%s/image".formatted(id));
        assertThat(location.getImageBucket()).isEqualTo("bucket-novo");
        assertThat(location.getImageObjectKey()).isEqualTo("locais/%s/nova.png".formatted(id));
        assertThat(location.getImageContentType()).isEqualTo("image/png");
        assertThat(location.getImageSizeBytes()).isEqualTo(2L);
        verify(imageStorageService).removeSilently("bucket-antigo", "locais/%s/antiga.png".formatted(id));
    }

    @Test
    void shouldRemoveImageOfLocation() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("bucket");
        location.setImageObjectKey("locais/%s/photo.png".formatted(id));
        location.setImageContentType("image/png");
        location.setImageSizeBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.removeImage(id);

        assertThat(response.imageUrl()).isNull();
        assertThat(location.getImageBucket()).isNull();
        assertThat(location.getImageObjectKey()).isNull();
        assertThat(location.getImageContentType()).isNull();
        assertThat(location.getImageSizeBytes()).isNull();
        verify(imageStorageService).removeSilently("bucket", "locais/%s/photo.png".formatted(id));
    }

    @Test
    void notShouldRemoveObjectWhenLocationNotHasImage() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(repository.save(any(StorageLocation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.removeImage(id);

        verify(imageStorageService).removeSilently(null, null);
        verify(imageStorageService, never()).storeLocationImage(any(), any());
    }

    @Test
    void shouldFindMetadataAndOpenImageOfLocation() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);
        location.setImageBucket("stella-locais");
        location.setImageObjectKey("locais/%s/photo.png".formatted(id));
        location.setImageContentType("image/png");
        location.setImageSizeBytes(2L);

        when(repository.findById(id)).thenReturn(Optional.of(location));
        when(imageStorageService.open("stella-locais", "locais/%s/photo.png".formatted(id)))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));

        var metadados = service.fetchImageMetadata(id);
        var image = service.openImage(id);

        assertThat(metadados.contentType()).isEqualTo("image/png");
        assertThat(metadados.sizeBytes()).isEqualTo(2L);
        assertThat(image).hasSameContentAs(new ByteArrayInputStream(new byte[]{1, 2}));
    }

    @Test
    void shouldRejectMetadataWhenLocationNotHasImage() {
        UUID id = UUID.randomUUID();
        StorageLocation location = location(id, "Deposito", null);

        when(repository.findById(id)).thenReturn(Optional.of(location));

        assertThatThrownBy(() -> service.fetchImageMetadata(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location does not have an image.");
    }

    private StorageLocation location(UUID id, String name, StorageLocation parent) {
        StorageLocation location = new StorageLocation();
        location.setId(id);
        location.setName(name);
        location.setParent(parent);
        location.setActive(true);
        return location;
    }
}
