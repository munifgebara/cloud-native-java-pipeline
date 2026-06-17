package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.ConsultaSemanticaItemDTO;
import br.com.stella.api.dto.MainItemCreateDTO;
import br.com.stella.api.dto.ImagemItemMestreDTO;
import br.com.stella.api.dto.MainItemResponseDTO;
import br.com.stella.api.dto.MainItemSummaryDTO;
import br.com.stella.api.dto.MainItemUpdateDTO;
import br.com.stella.api.entity.Category;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.mapper.ItemMestreMapper;
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

    private final CategoryRepository categoriaRepository;
    private final MainItemImageStorageService imagemStorageService;
    private final MainItemVectorSearchService vectorSearchService;

    public MainItemService(
            MainItemRepository repository,
            EntityManager entityManager,
            CategoryRepository categoriaRepository,
            MainItemImageStorageService imagemStorageService,
            MainItemVectorSearchService vectorSearchService
    ) {
        super(repository, entityManager, MainItem.class);
        this.categoriaRepository = categoriaRepository;
        this.imagemStorageService = imagemStorageService;
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
    public MainItemResponseDTO criar(MainItemCreateDTO dto) {
        MainItem item = ItemMestreMapper.toEntity(dto);
        normalizarCampos(item);
        item.setCategory(buscarCategoriaAtiva(dto.categoriaId()));

        MainItem salvo = salvar(item);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setActive(false);
            salvo = salvar(salvo);
        }
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-create");
        StructuredBusinessLogger.info(log, "inventory", "item-created", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "category_id", salvo.getCategory() == null ? null : salvo.getCategory().getId(),
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the full DTO of a main item by its identifier.
     *
     * @param id UUID of the main item
     * @return full DTO of the item
     * @throws jakarta.persistence.EntityNotFoundException if the item does not exist
     */
    @Transactional(readOnly = true)
    public MainItemResponseDTO buscarResponsePorId(UUID id) {
        return ItemMestreMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lists all active main items in alphabetical order by name.
     *
     * @return list of summary DTOs of active items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> listarResumo() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all main items, including deactivated ones.
     *
     * @return list of summary DTOs of all items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> listarResumoIncluindoInativos() {
        return findAllIncludingInactive().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Finds active main items whose name contains the given text (case-insensitive).
     *
     * @param nome substring to search; returns empty list if blank
     * @return list of summary DTOs of the found items
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> buscarPorNome(String nome) {
        String nomeTratado = BrValidations.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(nomeTratado).stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Filters active main items combining name and category with {@code AND}.
     * Null parameters are ignored.
     *
     * @param nome        name substring; ignored if {@code null} or blank
     * @param categoriaId UUID of the category; ignored if {@code null}
     * @return list of summary DTOs of items matching the filters
     */
    @Transactional(readOnly = true)
    public List<MainItemSummaryDTO> filtrar(String nome, UUID categoriaId) {
        return repository.findAll(
                        MainItemRepository.filtrarAtivos(BrValidations.trimToNull(nome), categoriaId),
                        Sort.by("nome").ascending()
                ).stream()
                .map(ItemMestreMapper::toResumoDTO)
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
    public MainItemResponseDTO atualizar(UUID id, MainItemUpdateDTO dto) {
        MainItem item = buscarPorId(id);
        Category category = buscarCategoriaAtiva(dto.categoriaId());

        ItemMestreMapper.updateEntity(item, dto);
        normalizarCampos(item);
        item.setCategory(category);

        MainItem salvo = salvar(item);
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-update");
        StructuredBusinessLogger.info(log, "inventory", "item-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "category_id", salvo.getCategory() == null ? null : salvo.getCategory().getId(),
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Updates the main image of a main item with a file uploaded by the user.
     * Equivalent to calling {@link #atualizarImagemPrincipal(UUID, MultipartFile, boolean, String)}
     * with {@code generatedByAi = false}.
     *
     * @param id      UUID of the main item
     * @param arquivo image file uploaded by the client
     * @return full DTO of the item with the new image metadata
     */
    @Transactional
    public MainItemResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo) {
        return atualizarImagemPrincipal(id, arquivo, false, null);
    }

    /**
     * Updates the main image of a main item, storing it in MinIO and updating the metadata.
     * The previous image is removed from the bucket after successful saving of the new one.
     * At the end, the vector index is re-synchronized.
     *
     * @param id            UUID of the main item
     * @param arquivo       uploaded image file
     * @param generatedByAi {@code true} if the image was generated by AI
     * @param provider      name of the AI provider (e.g.: "openai"); ignored if {@code generatedByAi} is {@code false}
     * @return full DTO of the item with the new image metadata
     */
    @Transactional
    public MainItemResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo, boolean generatedByAi, String provider) {
        MainItem item = buscarPorId(id);
        String bucketAnterior = item.getImagemBucket();
        String objectKeyAnterior = item.getImagemObjectKey();

        ImagemItemMestreDTO imagem = imagemStorageService.armazenar(id, arquivo);
        item.setImagemBucket(imagem.bucket());
        item.setImagemObjectKey(imagem.objectKey());
        item.setImagemContentType(imagem.contentType());
        item.setImagemTamanhoBytes(imagem.tamanhoBytes());
        item.setImagemGeneratedByAi(generatedByAi);
        item.setImagemProvider(generatedByAi ? BrValidations.trimToNull(provider) : null);

        MainItem salvo = salvar(item);
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-image-update");
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "item-image-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getName(),
                "image_content_type", imagem.contentType(),
                "image_size_bytes", imagem.tamanhoBytes(),
                "image_generated_by_ai", generatedByAi,
                "ai_provider", generatedByAi ? BrValidations.trimToNull(provider) : null,
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the metadata (bucket, objectKey, contentType and size) of the main image of an item.
     *
     * @param id UUID of the main item
     * @return DTO with the metadata required to retrieve the file from MinIO
     * @throws IllegalArgumentException if the item does not have a registered image
     */
    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagemPrincipal(UUID id) {
        MainItem item = buscarPorId(id);
        if (item.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Main item does not have a main image.");
        }
        return new ImagemItemMestreDTO(
                item.getImagemBucket(),
                item.getImagemObjectKey(),
                item.getImagemContentType(),
                item.getImagemTamanhoBytes()
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
    public InputStream abrirImagemPrincipal(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagemPrincipal(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

    /**
     * Logically deactivates a main item and removes its entry from the vector index.
     *
     * @param id UUID of the item to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the item does not exist
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        MainItem item = buscarPorId(id);
        excluir(id);
        removerIndiceVetorialSilenciosamente(id, item.getName());
        StructuredBusinessLogger.info(log, "inventory", "item-deactivated", StructuredBusinessLogger.fields(
                "item_id", id,
                "item_name", item.getName(),
                "success", true
        ));
    }

    /**
     * Performs semantic search in the main item vector index.
     *
     * @param consulta free-text describing what is being searched
     * @return list of results ordered by semantic similarity
     */
    @Transactional(readOnly = true)
    public List<ConsultaSemanticaItemDTO> buscarSemanticamente(String consulta) {
        return vectorSearchService.buscar(consulta);
    }

    /**
     * Forces the vector re-indexing of all active main items.
     * Generates embeddings for each item and updates the pgvector index.
     *
     * @return number of re-indexed items
     */
    @Transactional
    public int reindexarBuscaSemantica() {
        return vectorSearchService.reindexarItensAtivos();
    }

    /**
     * Returns the previous revision history of a main item (Hibernate Envers).
     *
     * @param id UUID of the main item
     * @return list of revisions in chronological order; empty list if there is no history
     */
    @Transactional(readOnly = true)
    public List<RevisionDTO<MainItem>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private Category buscarCategoriaAtiva(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }

        Category category = categoriaRepository.findById(categoriaId)
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

    private void sincronizarIndiceVetorialSilenciosamente(MainItem item, String action) {
        executarAposCommit(action, item == null ? null : item.getId(), item == null ? null : item.getName(),
                () -> vectorSearchService.sincronizar(item));
    }

    private void removerIndiceVetorialSilenciosamente(UUID id, String nome) {
        executarAposCommit("item-index-remove-after-delete", id, nome, () -> vectorSearchService.remover(id));
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
