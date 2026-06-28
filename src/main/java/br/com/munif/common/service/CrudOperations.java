package br.com.munif.common.service;

import java.util.List;
import java.util.UUID;

/**
 * Typed contract for the CRUD operations exposed by the standard REST resources.
 *
 * @param <SUMMARY> summary DTO used in listings
 * @param <RESPONSE> full resource DTO
 * @param <CREATE> creation DTO
 * @param <UPDATE> update DTO
 * @param <REVISION> revision DTO
 */
public interface CrudOperations<SUMMARY, RESPONSE, CREATE, UPDATE, REVISION> {

    /**
     * Creates a resource from the supplied DTO.
     * @param dto creation data
     * @return created resource
     */
    RESPONSE create(CREATE dto);

    /**
     * Returns one resource visible to the current owner.
     * @param id resource identifier
     * @return visible resource
     */
    RESPONSE findResponseById(UUID id);

    /**
     * Lists active resources visible to the current owner.
     * @return active resource summaries
     */
    List<SUMMARY> listSummary();

    /**
     * Updates a resource owned by the current owner.
     * @param id resource identifier
     * @param dto update data
     * @return updated resource
     */
    RESPONSE update(UUID id, UPDATE dto);

    /**
     * Logically deletes a resource owned by the current owner.
     * @param id resource identifier
     */
    void deleteLogically(UUID id);

    /**
     * Lists active and inactive resources visible to the current owner.
     * @return visible resource summaries
     */
    List<SUMMARY> listSummaryIncludingInactive();

    /**
     * Lists the visible revisions of one resource.
     * @param id resource identifier
     * @return visible revisions
     */
    List<REVISION> listRevisions(UUID id);
}
