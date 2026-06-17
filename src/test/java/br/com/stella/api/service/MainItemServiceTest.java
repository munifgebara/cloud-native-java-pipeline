package br.com.stella.api.service;

import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.repository.CategoryRepository;
import br.com.stella.api.repository.MainItemRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemMestreServiceTest {

    @Mock
    private MainItemRepository repository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MainItemImageStorageService imageStorageService;

    @Mock
    private MainItemVectorSearchService vectorSearchService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private MainItemService service;

    @Test
    void shouldCreateItemMainWithCategoryNormalizingFields() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId, "Eletronicos", "eletronicos", true);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new MainItemCreateDTO(
                "  Notebook Dell Latitude 5440  ",
                "  Notebook corporativo  ",
                "  Prepared for future instances  ",
                null,
                categoryId,
                true
        ));

        ArgumentCaptor<MainItem> captor = ArgumentCaptor.forClass(MainItem.class);
        verify(repository).save(captor.capture());

        MainItem itemSalvo = captor.getValue();
        assertThat(itemSalvo.getName()).isEqualTo("Notebook Dell Latitude 5440");
        assertThat(itemSalvo.getDescription()).isEqualTo("Notebook corporativo");
        assertThat(itemSalvo.getNotes()).isEqualTo("Prepared for future instances");
        assertThat(itemSalvo.getCategory()).isEqualTo(category);
        assertThat(response.categoryId()).isEqualTo(categoryId);
        assertThat(response.categoryName()).isEqualTo("Eletronicos");
        assertThat(response.categoryIcon()).isEqualTo("eletronicos");
        verify(vectorSearchService).synchronize(itemSalvo);
    }

    @Test
    void shouldCreateItemMainWithoutCategory() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

        assertThat(response.name()).isEqualTo("Cadeira ergonomica");
        assertThat(response.categoryId()).isNull();
        verify(categoryRepository, never()).findById(any(UUID.class));
    }

    @Test
    void shouldCreateItemMainSameWhenIndexVectorFailure() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new ExternalIntegrationException("pgvector unavailable"))
                .when(vectorSearchService).synchronize(any(MainItem.class));

        var response = service.create(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

        assertThat(response.name()).isEqualTo("Cadeira ergonomica");
        verify(repository).save(any(MainItem.class));
        verify(vectorSearchService).synchronize(any(MainItem.class));
    }

    @Test
    void shouldSynchronizeIndexVectorOnlyAfterCommit() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.create(new MainItemCreateDTO("Cadeira ergonomica", null, null, null, null, true));

            verify(vectorSearchService, never()).synchronize(any(MainItem.class));

            var synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);

            synchronizations.getFirst().afterCommit();

            verify(vectorSearchService).synchronize(any(MainItem.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldCreateItemMainInactiveAfterPersistenceInitial() {
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(new MainItemCreateDTO("Arquivo legado", null, null, null, null, false));

        assertThat(response.ativa()).isFalse();
        verify(repository).flush();
    }

    @Test
    void shouldUpdateItemMainWithCategory() {
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        MainItem item = item(id, "Antigo", null);
        Category category = category(categoryId, "Moveis", "moveis", true);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, new MainItemUpdateDTO(
                "  Cadeira ergonomica  ",
                "  Cadeira de escritorio  ",
                null,
                null,
                categoryId,
                false
        ));

        assertThat(response.name()).isEqualTo("Cadeira ergonomica");
        assertThat(response.description()).isEqualTo("Cadeira de escritorio");
        assertThat(response.categoryName()).isEqualTo("Moveis");
        assertThat(response.ativa()).isFalse();
        verify(vectorSearchService).synchronize(item);
    }

    @Test
    void shouldUpdateItemMainRemovingCategory() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", category(UUID.randomUUID(), "Eletronicos", null, true));

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(id, new MainItemUpdateDTO(" Notebook ", " ", " Observacao ", null, null, true));

        assertThat(response.name()).isEqualTo("Notebook");
        assertThat(response.description()).isNull();
        assertThat(response.notes()).isEqualTo("Observacao");
        assertThat(response.categoryId()).isNull();
        verify(categoryRepository, never()).findById(any(UUID.class));
    }

    @Test
    void shouldPreventCategoryInactiveInItemMain() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId, "Inativa", null, false);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> service.create(new MainItemCreateDTO("Furadeira", null, null, null, categoryId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category must be active");

        verify(repository, never()).save(any(MainItem.class));
    }

    @Test
    void shouldPreventCategoryNonExistentInItemMain() {
        UUID categoryId = UUID.randomUUID();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new MainItemCreateDTO("Furadeira", null, null, null, categoryId, true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Category not found.");

        verify(repository, never()).save(any(MainItem.class));
    }

    @Test
    void shouldListResumoAndInactive() {
        MainItem active = item(UUID.randomUUID(), "Notebook", null);
        MainItem inactive = item(UUID.randomUUID(), "Arquivo legado", null);
        inactive.setActive(false);

        when(repository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(active));
        when(repository.findAllIncludingInactive()).thenReturn(List.of(active, inactive));

        assertThat(service.listSummary()).extracting("name").containsExactly("Notebook");
        assertThat(service.listSummaryIncludingInactive()).extracting("ativa").containsExactly(true, false);
    }

    @Test
    void shouldFindByNameOnlyWhenFilterProvided() {
        MainItem item = item(UUID.randomUUID(), "Furadeira Bosch", null);

        when(repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("Furadeira")).thenReturn(List.of(item));

        assertThat(service.findByName("  ")).isEmpty();
        assertThat(service.findByName(" Furadeira ")).hasSize(1);
    }

    @Test
    void shouldFilterByNameAndCategory() {
        UUID categoryId = UUID.randomUUID();
        Category category = category(categoryId, "Eletronicos", "eletronicos", true);
        MainItem item = item(UUID.randomUUID(), "Notebook", category);

        when(repository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(item));

        var response = service.filtrar(" Notebook ", categoryId);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().name()).isEqualTo("Notebook");
    }

    @Test
    void shouldUpdateImageMainOfItemMain() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);
        item.setImageBucket("stella-itens");
        item.setImageObjectKey("main-items/antiga.jpg");
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        var image = new MainItemImageDTO("stella-itens", "main-items/nova.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imageStorageService.storeMainItemImage(id, file)).thenReturn(image);
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateMainImage(id, file);

        assertThat(response.imageUrl()).isEqualTo("/api/public/main-items/%s/main-image".formatted(id));
        assertThat(response.imageContentType()).isEqualTo("image/png");
        assertThat(response.imageSizeBytes()).isEqualTo(3L);
        assertThat(response.imageGeneratedByAi()).isFalse();
        assertThat(response.imageProvider()).isNull();
        verify(imageStorageService).removeSilently("stella-itens", "main-items/antiga.jpg");
    }

    @Test
    void shouldUpdateImageMainMarkingOriginIa() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);
        var file = new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1, 2, 3});
        var image = new MainItemImageDTO("stella-itens", "main-items/ia.png", "image/png", 3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imageStorageService.storeMainItemImage(id, file)).thenReturn(image);
        when(repository.save(any(MainItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.updateMainImage(id, file, true, " openai ");

        assertThat(response.imageGeneratedByAi()).isTrue();
        assertThat(response.imageProvider()).isEqualTo("openai");
    }

    @Test
    void shouldRemoveIndexVectorOnExcluirLogically() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);

        service.deleteLogically(id);

        assertThat(item.isActive()).isFalse();
        verify(vectorSearchService).remove(id);
    }

    @Test
    void shouldExcluirLogicallySameWhenRemovalOfIndexVectorFailure() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(repository.save(item)).thenReturn(item);
        doThrow(new ExternalIntegrationException("pgvector unavailable")).when(vectorSearchService).remove(id);

        service.deleteLogically(id);

        assertThat(item.isActive()).isFalse();
        verify(vectorSearchService).remove(id);
    }

    @Test
    void shouldFindMetadataAndOpenImageMain() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);
        item.setImageBucket("stella-itens");
        item.setImageObjectKey("main-items/%s/photo.png".formatted(id));
        item.setImageContentType("image/png");
        item.setImageSizeBytes(3L);

        when(repository.findById(id)).thenReturn(Optional.of(item));
        when(imageStorageService.open("stella-itens", "main-items/%s/photo.png".formatted(id)))
                .thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        var metadados = service.fetchMainImageMetadata(id);
        var image = service.openMainImage(id);

        assertThat(metadados.objectKey()).isEqualTo("main-items/%s/photo.png".formatted(id));
        assertThat(metadados.tamanhoBytes()).isEqualTo(3L);
        assertThat(image).hasSameContentAs(new ByteArrayInputStream(new byte[]{1, 2, 3}));
    }

    @Test
    void shouldRejectMetadataWhenItemMainNotHasImageMain() {
        UUID id = UUID.randomUUID();
        MainItem item = item(id, "Notebook", null);

        when(repository.findById(id)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> service.fetchMainImageMetadata(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Main item does not have a main image.");
    }

    private MainItem item(UUID id, String name, Category category) {
        MainItem item = new MainItem();
        item.setId(id);
        item.setName(name);
        item.setCategory(category);
        item.setActive(true);
        return item;
    }

    private Category category(UUID id, String name, String icon, boolean ativa) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setIcon(icon);
        category.setActive(ativa);
        return category;
    }
}
