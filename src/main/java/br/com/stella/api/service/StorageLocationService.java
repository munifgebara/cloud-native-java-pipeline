package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.ImagemItemMestreDTO;
import br.com.stella.api.dto.StorageLocationResponseDTO;
import br.com.stella.api.dto.StorageLocationSummaryDTO;
import br.com.stella.api.dto.StorageLocationUpdateDTO;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.mapper.StorageLocationMapper;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.StorageLocationRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for business operations on {@link StorageLocation}.
 *
 * <p>Manages the lifecycle of inventory storage locations, which can be
 * organized in a parent-child hierarchy (e.g.: Building > Room > Cabinet).
 * Includes support for image upload via MinIO and audit revision queries.</p>
 *
 * <p>The listing returns locations already sorted depth-first (DFS),
 * with the full path of each node to facilitate display in hierarchical lists.</p>
 */
@Service
public class StorageLocationService extends SuperService<StorageLocation, StorageLocationRepository> {

    private static final Logger log = LoggerFactory.getLogger(StorageLocationService.class);

    private final MainItemImageStorageService imagemStorageService;

    public StorageLocationService(
            StorageLocationRepository repository,
            EntityManager entityManager,
            MainItemImageStorageService imagemStorageService
    ) {
        super(repository, entityManager, StorageLocation.class);
        this.imagemStorageService = imagemStorageService;
    }

    /**
     * Creates a new storage location.
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created location
     * @throws IllegalArgumentException if the provided parent location does not exist or is inactive
     */
    @Transactional
    public StorageLocationResponseDTO criar(StorageLocationCreateDTO dto) {
        StorageLocation location = StorageLocationMapper.toEntity(dto);
        normalizarCampos(location);
        location.setParent(buscarPaiAtivo(dto.paiId()));

        StorageLocation salvo = salvar(location);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setActive(false);
            salvo = salvar(salvo);
        }
        StructuredBusinessLogger.info(log, "inventory", "location-created", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getName(),
                "parent_location_id", salvo.getParent() == null ? null : salvo.getParent().getId(),
                "success", true
        ));
        return StorageLocationMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the full DTO of a location by its identifier.
     *
     * @param id UUID of the location
     * @return full DTO of the location
     * @throws jakarta.persistence.EntityNotFoundException if the location does not exist
     */
    @Transactional(readOnly = true)
    public StorageLocationResponseDTO buscarResponsePorId(UUID id) {
        return StorageLocationMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lists all active locations in hierarchical order (DFS), with the path and level of each node.
     *
     * @return list of summary DTOs of active locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> listarResumo() {
        return montarHierarquia(repository.findByActiveTrueOrderByNameAsc());
    }

    /**
     * Lists all locations, including deactivated ones, in hierarchical order (DFS).
     *
     * @return list of summary DTOs of all locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> listarResumoIncluindoInativos() {
        return montarHierarquia(findAllIncludingInactive());
    }

    /**
     * Finds active locations whose name contains the given text (case-insensitive),
     * returning the list in hierarchical order.
     *
     * @param nome substring to search in the location name; returns empty list if blank
     * @return list of summary DTOs of the found locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> buscarPorNome(String nome) {
        String nomeTratado = BrValidations.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return montarHierarquia(repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado));
    }

    /**
     * Updates the data of an existing location, validating the parent-child hierarchy.
     *
     * @param id  UUID of the location to update
     * @param dto update data validated by Bean Validation
     * @return full DTO of the updated location
     * @throws IllegalArgumentException if the parent location is inactive, is the location itself,
     *                                  or is a descendant of the location being updated
     */
    @Transactional
    public StorageLocationResponseDTO atualizar(UUID id, StorageLocationUpdateDTO dto) {
        StorageLocation location = buscarPorId(id);
        StorageLocation pai = buscarPaiAtivo(dto.paiId());
        validarHierarquia(location, pai);

        StorageLocationMapper.updateEntity(location, dto);
        normalizarCampos(location);
        location.setParent(pai);

        StorageLocation salvo = salvar(location);
        StructuredBusinessLogger.info(log, "inventory", "location-updated", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getName(),
                "parent_location_id", salvo.getParent() == null ? null : salvo.getParent().getId(),
                "success", true
        ));
        return StorageLocationMapper.toResponseDTO(salvo);
    }

    /**
     * Updates the location image, storing it in MinIO and removing the previous one.
     *
     * @param id      UUID of the location
     * @param arquivo image file sent by the client
     * @return full DTO of the location with the new image metadata
     */
    @Transactional
    public StorageLocationResponseDTO atualizarImagem(UUID id, MultipartFile arquivo) {
        StorageLocation location = buscarPorId(id);
        String bucketAnterior = location.getImagemBucket();
        String objectKeyAnterior = location.getImagemObjectKey();

        ImagemItemMestreDTO imagem = imagemStorageService.armazenarLocal(id, arquivo);
        location.setImagemBucket(imagem.bucket());
        location.setImagemObjectKey(imagem.objectKey());
        location.setImagemContentType(imagem.contentType());
        location.setImagemTamanhoBytes(imagem.tamanhoBytes());

        StorageLocation salvo = salvar(location);
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "location-image-updated", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getName(),
                "image_content_type", imagem.contentType(),
                "image_size_bytes", imagem.tamanhoBytes(),
                "success", true
        ));
        return StorageLocationMapper.toResponseDTO(salvo);
    }

    /**
     * Removes the location image, deleting it from MinIO and clearing the metadata.
     *
     * @param id UUID of the location
     * @return full DTO of the location without image metadata
     */
    @Transactional
    public StorageLocationResponseDTO removerImagem(UUID id) {
        StorageLocation location = buscarPorId(id);
        String bucketAnterior = location.getImagemBucket();
        String objectKeyAnterior = location.getImagemObjectKey();

        location.setImagemBucket(null);
        location.setImagemObjectKey(null);
        location.setImagemContentType(null);
        location.setImagemTamanhoBytes(null);

        StorageLocation salvo = salvar(location);
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "location-image-removed", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getName(),
                "success", true
        ));
        return StorageLocationMapper.toResponseDTO(salvo);
    }

    /**
     * Returns the image metadata of a location (bucket, objectKey, contentType and size).
     *
     * @param id UUID of the location
     * @return DTO with the metadata required to retrieve the file from MinIO
     * @throws IllegalArgumentException if the location does not have a registered image
     */
    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagem(UUID id) {
        StorageLocation location = buscarPorId(id);
        if (location.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Location does not have an image.");
        }
        return new ImagemItemMestreDTO(
                location.getImagemBucket(),
                location.getImagemObjectKey(),
                location.getImagemContentType(),
                location.getImagemTamanhoBytes()
        );
    }

    /**
     * Opens a read stream for the location image in MinIO.
     * The caller is responsible for closing the stream after use.
     *
     * @param id UUID of the location
     * @return image read stream
     * @throws IllegalArgumentException if the location does not have a registered image
     */
    @Transactional(readOnly = true)
    public InputStream abrirImagem(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagem(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

    /**
     * Logically deactivates a storage location.
     *
     * @param id UUID of the location to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the location does not exist
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        StorageLocation location = buscarPorId(id);
        excluir(id);
        StructuredBusinessLogger.info(log, "inventory", "location-deactivated", StructuredBusinessLogger.fields(
                "location_id", id,
                "location_name", location.getName(),
                "success", true
        ));
    }

    /**
     * Returns the previous revision history of a location (Hibernate Envers).
     *
     * @param id UUID of the location
     * @return list of revisions in chronological order; empty list if there is no history
     */
    @Transactional(readOnly = true)
    public List<RevisionDTO<StorageLocation>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private List<StorageLocationSummaryDTO> montarHierarquia(List<StorageLocation> locais) {
        Map<UUID, List<StorageLocation>> filhosPorPai = locais.stream()
                .filter(location -> location.getParent() != null)
                .collect(Collectors.groupingBy(location -> location.getParent().getId()));

        List<StorageLocation> raizes = locais.stream()
                .filter(location -> location.getParent() == null || locais.stream().noneMatch(item -> item.getId().equals(location.getParent().getId())))
                .sorted(Comparator.comparing(StorageLocation::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<StorageLocationSummaryDTO> resultado = new ArrayList<>();
        Set<UUID> visitados = new HashSet<>();
        for (StorageLocation raiz : raizes) {
            adicionarNaHierarquia(raiz, raiz.getName(), 0, filhosPorPai, resultado, visitados);
        }
        return resultado;
    }

    private void adicionarNaHierarquia(
            StorageLocation location,
            String caminho,
            int nivel,
            Map<UUID, List<StorageLocation>> filhosPorPai,
            List<StorageLocationSummaryDTO> resultado,
            Set<UUID> visitados
    ) {
        if (!visitados.add(location.getId())) {
            return;
        }

        resultado.add(StorageLocationMapper.toResumoDTO(location, caminho, nivel));
        filhosPorPai.getOrDefault(location.getId(), List.of()).stream()
                .sorted(Comparator.comparing(StorageLocation::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(filho -> adicionarNaHierarquia(filho, caminho + " > " + filho.getName(), nivel + 1, filhosPorPai, resultado, visitados));
    }

    private StorageLocation buscarPaiAtivo(UUID paiId) {
        if (paiId == null) {
            return null;
        }

        StorageLocation pai = buscarPorId(paiId);
        if (!pai.isActive()) {
            throw new IllegalArgumentException("Parent location must be active.");
        }
        return pai;
    }

    private void validarHierarquia(StorageLocation location, StorageLocation pai) {
        if (pai == null) {
            return;
        }

        if (location.getId().equals(pai.getId())) {
            throw new IllegalArgumentException("A location cannot be its own parent.");
        }

        StorageLocation atual = pai.getParent();
        while (atual != null) {
            if (location.getId().equals(atual.getId())) {
                throw new IllegalArgumentException("Parent location cannot be a descendant of itself.");
            }
            atual = atual.getParent();
        }
    }

    private void normalizarCampos(StorageLocation location) {
        location.setName(BrValidations.trimToNull(location.getName()));
        location.setDescription(BrValidations.trimToNull(location.getDescription()));
    }
}
