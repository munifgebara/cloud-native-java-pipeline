package br.com.stella.api.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.StorageLocationCreateDTO;
import br.com.stella.api.dto.MainItemImageDTO;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private final MainItemImageStorageService imageStorageService;

    public StorageLocationService(
            StorageLocationRepository repository,
            EntityManager entityManager,
            MainItemImageStorageService imageStorageService
    ) {
        super(repository, entityManager, StorageLocation.class);
        this.imageStorageService = imageStorageService;
    }

    /**
     * Creates a new storage location.
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created location
     * @throws IllegalArgumentException if the provided parent location does not exist or is inactive
     */
    @Transactional
    public StorageLocationResponseDTO create(StorageLocationCreateDTO dto) {
        StorageLocation location = StorageLocationMapper.toEntity(dto);
        normalizeFields(location);
        location.setParent(findActiveParent(dto.parentId()));

        StorageLocation salvo = saveWithRequestedActiveState(location, dto.active());
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
    public StorageLocationResponseDTO findResponseById(UUID id) {
        return StorageLocationMapper.toResponseDTO(findById(id));
    }

    /**
     * Lists all active locations in hierarchical order (DFS), with the path and level of each node.
     *
     * @return list of summary DTOs of active locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> listSummary() {
        return buildHierarchy(repository.findAllActive(Sort.by("name")));
    }

    /**
     * Lists all locations, including deactivated ones, in hierarchical order (DFS).
     *
     * @return list of summary DTOs of all locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> listSummaryIncludingInactive() {
        return buildHierarchy(findAllIncludingInactive());
    }

    /**
     * Finds active locations whose name contains the given text (case-insensitive),
     * returning the list in hierarchical order.
     *
     * @param name substring to search in the location name; returns empty list if blank
     * @return list of summary DTOs of the found locations
     */
    @Transactional(readOnly = true)
    public List<StorageLocationSummaryDTO> findByName(String name) {
        String normalizedName = BrValidations.trimToNull(name);
        if (normalizedName == null) {
            return List.of();
        }

        return buildHierarchy(repository.findAll(activeNameContains(normalizedName), Sort.by("name")));
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
    public StorageLocationResponseDTO update(UUID id, StorageLocationUpdateDTO dto) {
        StorageLocation location = findById(id);
        StorageLocation parent = findActiveParent(dto.parentId());
        validarHierarquia(location, parent);

        StorageLocationMapper.updateEntity(location, dto);
        normalizeFields(location);
        location.setParent(parent);

        StorageLocation salvo = save(location);
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
     * @param file image file sent by the client
     * @return full DTO of the location with the new image metadata
     */
    @Transactional
    public StorageLocationResponseDTO updateImage(UUID id, MultipartFile file) {
        StorageLocation location = findById(id);
        String bucketAnterior = location.getImageBucket();
        String objectKeyAnterior = location.getImageObjectKey();

        MainItemImageDTO image = imageStorageService.storeLocationImage(id, file);
        location.setImageBucket(image.bucket());
        location.setImageObjectKey(image.objectKey());
        location.setImageContentType(image.contentType());
        location.setImageSizeBytes(image.sizeBytes());

        StorageLocation salvo = save(location);
        imageStorageService.removeSilently(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "location-image-updated", StructuredBusinessLogger.fields(
                "location_id", salvo.getId(),
                "location_name", salvo.getName(),
                "image_content_type", image.contentType(),
                "image_size_bytes", image.sizeBytes(),
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
    public StorageLocationResponseDTO removeImage(UUID id) {
        StorageLocation location = findById(id);
        String bucketAnterior = location.getImageBucket();
        String objectKeyAnterior = location.getImageObjectKey();

        location.setImageBucket(null);
        location.setImageObjectKey(null);
        location.setImageContentType(null);
        location.setImageSizeBytes(null);

        StorageLocation salvo = save(location);
        imageStorageService.removeSilently(bucketAnterior, objectKeyAnterior);
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
    public MainItemImageDTO fetchImageMetadata(UUID id) {
        StorageLocation location = findById(id);
        if (location.getImageObjectKey() == null) {
            throw new IllegalArgumentException("Location does not have an image.");
        }
        return new MainItemImageDTO(
                location.getImageBucket(),
                location.getImageObjectKey(),
                location.getImageContentType(),
                location.getImageSizeBytes()
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
    public InputStream openImage(UUID id) {
        MainItemImageDTO image = fetchImageMetadata(id);
        return imageStorageService.open(image.bucket(), image.objectKey());
    }

    /**
     * Logically deactivates a storage location.
     *
     * @param id UUID of the location to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the location does not exist
     */
    @Transactional
    public void deleteLogically(UUID id) {
        StorageLocation location = findById(id);
        delete(id);
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
    public List<RevisionDTO<StorageLocation>> listRevisions(UUID id) {
        return listPreviousVersions(id);
    }

    private List<StorageLocationSummaryDTO> buildHierarchy(List<StorageLocation> locations) {
        Map<UUID, List<StorageLocation>> childrenByParent = locations.stream()
                .filter(location -> location.getParent() != null)
                .collect(Collectors.groupingBy(location -> location.getParent().getId()));

        List<StorageLocation> raizes = locations.stream()
                .filter(location -> location.getParent() == null || locations.stream().noneMatch(item -> item.getId().equals(location.getParent().getId())))
                .sorted(Comparator.comparing(StorageLocation::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        List<StorageLocationSummaryDTO> result = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        for (StorageLocation root : raizes) {
            addToHierarchy(root, root.getName(), 0, childrenByParent, result, visited);
        }
        return result;
    }

    private Specification<StorageLocation> activeNameContains(String name) {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%")
        );
    }

    private void addToHierarchy(
            StorageLocation location,
            String path,
            int level,
            Map<UUID, List<StorageLocation>> childrenByParent,
            List<StorageLocationSummaryDTO> result,
            Set<UUID> visited
    ) {
        if (!visited.add(location.getId())) {
            return;
        }

        result.add(StorageLocationMapper.toResumoDTO(location, path, level));
        childrenByParent.getOrDefault(location.getId(), List.of()).stream()
                .sorted(Comparator.comparing(StorageLocation::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(child -> addToHierarchy(child, path + " > " + child.getName(), level + 1, childrenByParent, result, visited));
    }

    private StorageLocation findActiveParent(UUID parentId) {
        if (parentId == null) {
            return null;
        }

        StorageLocation parent = findById(parentId);
        if (!parent.isActive()) {
            throw new IllegalArgumentException("Parent location must be active.");
        }
        return parent;
    }

    private void validarHierarquia(StorageLocation location, StorageLocation parent) {
        if (parent == null) {
            return;
        }

        if (location.getId().equals(parent.getId())) {
            throw new IllegalArgumentException("A location cannot be its own parent.");
        }

        StorageLocation atual = parent.getParent();
        while (atual != null) {
            if (location.getId().equals(atual.getId())) {
                throw new IllegalArgumentException("Parent location cannot be a descendant of itself.");
            }
            atual = atual.getParent();
        }
    }

    private void normalizeFields(StorageLocation location) {
        location.setName(BrValidations.trimToNull(location.getName()));
        location.setDescription(BrValidations.trimToNull(location.getDescription()));
    }
}
