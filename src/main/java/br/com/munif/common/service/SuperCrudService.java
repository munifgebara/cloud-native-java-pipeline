package br.com.munif.common.service;

import br.com.munif.common.dto.RevisionDTO;
import br.com.munif.common.persistencia.BaseEntity;
import br.com.munif.common.persistencia.SuperRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Base service for resources that expose the standard CRUD contract.
 *
 * <p>Domain services still implement creation and update rules. This class only
 * centralizes owner-scoped reads, soft deletion and DTO conversion.</p>
 *
 * @param <T> entity type
 * @param <R> owner-scoped repository type
 * @param <SUMMARY> summary DTO type
 * @param <RESPONSE> full response DTO type
 * @param <CREATE> creation DTO type
 * @param <UPDATE> update DTO type
 * @param <REVISION> revision DTO type
 */
public abstract class SuperCrudService<
        T extends BaseEntity,
        R extends SuperRepository<T>,
        SUMMARY,
        RESPONSE,
        CREATE,
        UPDATE,
        REVISION>
        extends SuperService<T, R>
        implements CrudOperations<SUMMARY, RESPONSE, CREATE, UPDATE, REVISION> {

    /**
     * Creates a CRUD service using the owner-scoped repository and entity metadata.
     * @param repository owner-scoped repository
     * @param entityManager JPA entity manager used by revision queries
     * @param entityClass entity class
     */
    protected SuperCrudService(R repository, EntityManager entityManager, Class<T> entityClass) {
        super(repository, entityManager, entityClass);
    }

    @Override
    @Transactional(readOnly = true)
    public RESPONSE findResponseById(java.util.UUID id) {
        return toResponse(findById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SUMMARY> listSummary() {
        return toSummaries(repository.findAllActive(defaultSort()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SUMMARY> listSummaryIncludingInactive() {
        return toSummaries(findAllIncludingInactive());
    }

    @Override
    @Transactional
    public void deleteLogically(java.util.UUID id) {
        delete(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<REVISION> listRevisions(java.util.UUID id) {
        return listPreviousVersions(id).stream()
                .map(this::toRevision)
                .toList();
    }

    /**
     * Returns the default ordering for active CRUD listings.
     * @return default sort
     */
    protected Sort defaultSort() {
        return Sort.by("name");
    }

    /**
     * Converts a list of entities to summary DTOs.
     * @param entities entities to convert
     * @return summary DTOs
     */
    protected List<SUMMARY> toSummaries(List<T> entities) {
        return entities.stream().map(this::toSummary).toList();
    }

    /**
     * Converts one entity to its summary DTO.
     * @param entity entity to convert
     * @return summary DTO
     */
    protected abstract SUMMARY toSummary(T entity);

    /**
     * Converts one entity to its full response DTO.
     * @param entity entity to convert
     * @return full response DTO
     */
    protected abstract RESPONSE toResponse(T entity);

    @SuppressWarnings("unchecked")
    /**
     * Converts a generic Envers revision to the resource revision DTO.
     * @param revision generic revision
     * @return resource revision DTO
     */
    protected REVISION toRevision(RevisionDTO<T> revision) {
        return (REVISION) revision;
    }
}
