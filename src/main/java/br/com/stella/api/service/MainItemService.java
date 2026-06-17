package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.SemanticSearchItemDTO;
import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.MainItemImageDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.mapper.MainItemMapper;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.CategoryRepository;
import br.com.stella.api.repository.MainItemRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for business operations on {@link MainItem}.
 *
 * <p>Manages the lifecycle of inventory main items, including persistence,
 * main image upload to MinIO, and synchronization of the semantic search index
 * (pgvector) after each change.</p>
 *
 * <p>Vector synchronization is executed <em>after the transaction commit</em> to ensure
 * that the index only reflects data already confirmed in the relational database.</p>
 */
@Service
public class MainItemService extends SuperService<MainItem, MainItemRepository> {

    private static final Logger log = LoggerFactory.getLogger(MainItemService.class);

    private final CategoryRepository categoryRepository;
    private final MainItemImageStorageService imageStorageService;
    private final MainItemVectorSearchService vectorSearchService;

    public MainItemService(
            MainItemRepository repository,
            EntityManager entityManager,
            CategoryRepository categoryRepository,
            MainItemImageStorageService imageStorageService,
            MainItemVectorSearchService vectorSearchService
    ) {
        super(repository, entityManager, MainItem.class);
        this.categoryRepository = categoryRepository;
        this.imageStorageService = imageStorageService;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Creates a new main item and schedules the vector index synchronization.
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created item
     * @throws IllegalArgumentException if the provided category does not exist or is inactive
     */
    @Transactional
    public MainItemResponseDTO create(MainItemCreateDTO dto) {
        MainItem item = MainItemMapper.toEntity(dto);
        normalizarCampos(item);
        item.setCategory(findActiveCategory(dto.categoryId()));

        MainItem salvo = save(item);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setActive(false);
            salvo = save(salvo);
        }
        syncVectorIndexSilently(salvo, "item-index-sync-after-create");
        StructuredBusinessLogger.info(log, "inventory", "item-created", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "category_id", salvo.getCategory() == null ? null : salvo.getCategory().getId(),
                "success", true
        ));
        return MainItemMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the full DTO of a main item by its identifier.
     *
     * @param id UUID of the main item
     * @return full DTO of the item
     * @throws jakarta.persistence.EntityNotFoundException if the item does not exist
     */
    @Transactional(readOnly = true)
    public MainItemResponseDTO findResponseById(UUID id) {
        return MainItemMapper.toResponseDTO(findById(id));
    }

    /**
     * Lists all active main items in alphabetical order by name.
     *
     * @return list of summary DTOs of active items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> listSummary() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(MainItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all main items, including deactivated ones.
     *
     * @return list of summary DTOs of all items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> listSummaryIncludingInactive() {
        return findAllIncludingInactive().stream()
                .map(MainItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active main items whose name contains the given text (case-insensitive).
     *
     * @param name substring to search; returns empty list if blank
     * @return list of summary DTOs of the found items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> findByName(String name) {
        String normalizedName = BrValidations.trimToNull(name);
        if (normalizedName == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(normalizedName).stream()
                .map(MainItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Filters active main items combining name and category with {@code AND}.
     * Null parameters are ignored.
     *
     * @param name        name substring; ignored if {@code null} or blank
     * @param categoryId UUID of the category; ignored if {@code null}
     * @return list of summary DTOs of items matching the filters
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> filtrar(String name, UUID categoryId) {
        return repository.findAll(
                        MainItemRepository.filterActive(BrValidations.trimToNull(name), categoryId),
                        Sort.by("name").ascending()
                ).stream()
                .map(MainItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Updates the data of an existing main item and re-indexes the semantic search vector.
     *
     * @param id  UUID of the item to update
     * @param dto update data validated by Bean Validation
     * @return full DTO of the updated item
     * @throws jakarta.persistence.EntityNotFoundException if the item does not exist
     * @throws IllegalArgumentException if the provided category does not exist or is inactive
     */
    @Transactional
    public MainItemResponseDTO update(UUID id, MainItemUpdateDTO dto) {
        MainItem item = findById(id);
        Category category = findActiveCategory(dto.categoryId());

        MainItemMapper.updateEntity(item, dto);
        normalizarCampos(item);
        item.setCategory(category);

        MainItem salvo = save(item);
        syncVectorIndexSilently(salvo, "item-index-sync-after-update");
        StructuredBusinessLogger.info(log, "inventory", "item-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "category_id", salvo.getCategory() == null ? null : salvo.getCategory().getId(),
                "success", true
        ));
        return MainItemMapper.toResponseDTO(salvo);
    }

    /**
     * Updates the main image of a main item with a file uploaded by the user.
     * Equivalent to calling {@link #updateMainImage(UUID, MultipartFile, boolean, String)}
     * with {@code generatedByAi = false}.
     *
     * @param id      UUID of the main item
     * @param file image file uploaded by the client
     * @return full DTO of the item with the new image metadata
     */
    @Transactional
    public MainItemResponseDTO updateMainImage(UUID id, MultipartFile file) {
        return updateMainImage(id, file, false, null);
    }

    /**
     * Updates the main image of a main item, storing it in MinIO and updating the metadata.
     * The previous image is removed from the bucket after successful saving of the new one.
     * At the end, the vector index is re-synchronized.
     *
     * @param id            UUID of the main item
     * @param file       uploaded image file
     * @param generatedByAi {@code true} if the image was generated by AI
     * @param provider      name of the AI provider (e.g.: "openai"); ignored if {@code generatedByAi} is {@code false}
     * @return full DTO of the item with the new image metadata
     */
    @Transactional
    public MainItemResponseDTO updateMainImage(UUID id, MultipartFile file, boolean generatedByAi, String provider) {
        MainItem item = findById(id);
        String bucketAnterior = item.getImageBucket();
        String objectKeyAnterior = item.getImageObjectKey();

        MainItemImageDTO image = imageStorageService.armazenar(id, file);
        item.setImageBucket(image.bucket());
        item.setImageObjectKey(image.objectKey());
        item.setImageContentType(image.contentType());
        item.setImageSizeBytes(image.tamanhoBytes());
        item.setImageGeneratedByAi(generatedByAi);
        item.setImageProvider(generatedByAi ? BrValidations.trimToNull(provider) : null);

        MainItem salvo = save(item);
        syncVectorIndexSilently(salvo, "item-index-sync-after-image-update");
        imageStorageService.removeSilently(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "item-image-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "image_content_type", image.contentType(),
                "image_size_bytes", image.tamanhoBytes(),
                "image_generated_by_ai", generatedByAi,
                "ai_provider", generatedByAi ? BrValidations.trimToNull(provider) : null,
                "success", true
        ));
        return MainItemMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the metadata (bucket, objectKey, contentType and size) of the main image of an item.
     *
     * @param id UUID of the main item
     * @return DTO with the metadata required to retrieve the file from MinIO
     * @throws IllegalArgumentException if the item does not have a registered image
     */
    @Transactional(readOnly = true)
    public MainItemImageDTO fetchMainImageMetadata(UUID id) {
        MainItem item = findById(id);
        if (item.getImageObjectKey() == null) {
            throw new IllegalArgumentException("Main item does not have a main image.");
        }
        return new MainItemImageDTO(
                item.getImageBucket(),
                item.getImageObjectKey(),
                item.getImageContentType(),
                item.getImageSizeBytes()
        );
    }

    /**
     * Opens a read stream for the main image of the item in MinIO.
     * The caller is responsible for closing the stream after use.
     *
     * @param id UUID of the main item
     * @return image read stream
     * @throws IllegalArgumentException if the item does not have a registered image
     */
    @Transactional(readOnly = true)
    public InputStream openMainImage(UUID id) {
        MainItemImageDTO image = fetchMainImageMetadata(id);
        return imageStorageService.abrir(image.bucket(), image.objectKey());
    }

    /**
     * Logically deactivates a main item and removes its entry from the vector index.
     *
     * @param id UUID of the item to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the item does not exist
     */
    @Transactional
    public void deleteLogically(UUID id) {
        MainItem item = findById(id);
        delete(id);
        removeVectorIndexSilently(id, item.getName());
        StructuredBusinessLogger.info(log, "inventory", "item-deactivated", StructuredBusinessLogger.fields(
                "item_id", id,
                "item_name", item.getName(),
                "success", true
        ));
    }

    /**
     * Performs semantic search in the main item vector index.
     *
     * @param query free-text describing what is being searched
     * @return list of results ordered by semantic similarity
     */
    @Transactional(readOnly = true)
    public List<SemanticSearchItemDTO> searchSemantically(String query) {
        return vectorSearchService.search(query);
    }

    /**
     * Forces the vector re-indexing of all active main items.
     * Generates embeddings for each item and updates the pgvector index.
     *
     * @return number of re-indexed items
     */
    @Transactional
    public int reindexSemanticSearch() {
        return vectorSearchService.reindexActiveItems();
    }

    /**
     * Returns the previous revision history of a main item (Hibernate Envers).
     *
     * @param id UUID of the main item
     * @return list of revisions in chronological order; empty list if there is no history
     */
    @Transactional(readOnly = true)
    public List<RevisionDTO<MainItem>> listRevisions(UUID id) {
        return listPreviousVersions(id);
    }

    private Category findActiveCategory(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        if (!category.isActive()) {
            throw new IllegalArgumentException("Category must be active.");
        }
        return category;
    }

    private void normalizarCampos(MainItem item) {
        item.setName(BrValidations.trimToNull(item.getName()));
        item.setDescription(BrValidations.trimToNull(item.getDescription()));
        item.setNotes(BrValidations.trimToNull(item.getNotes()));
    }

    private void syncVectorIndexSilently(MainItem item, String action) {
        executarAposCommit(action, item == null ? null : item.getId(), item == null ? null : item.getName(),
                () -> vectorSearchService.sincronizar(item));
    }

    private void removeVectorIndexSilently(UUID id, String name) {
        executarAposCommit("item-index-remove-after-delete", id, name, () -> vectorSearchService.remover(id));
    }

    private void executarAposCommit(String action, UUID itemId, String itemName, Runnable operation) {
        Runnable guardedOperation = () -> {
            try {
                operation.run();
            } catch (RuntimeException ex) {
                StructuredBusinessLogger.warn(log, "vector-search", action, StructuredBusinessLogger.fields(
                        "item_id", itemId,
                        "item_name", itemName,
                        "success", false,
                        "failure_type", ex.getClass().getSimpleName()
                ));
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guardedOperation.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guardedOperation.run();
            }
        });
    }
}
