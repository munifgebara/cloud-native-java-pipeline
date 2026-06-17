package br.com.munif.common.controller;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

/**
 * Base contract for the system's REST controllers.
 *
 * <p>Defines the standard CRUD endpoints that must be implemented by each
 * concrete controller. The use of generic type parameters allows each
 * resource to expose its own DTOs (summary, response, create, and update)
 * without duplicating the method structure.</p>
 *
 * <p><strong>Type parameter naming convention:</strong></p>
 * <ul>
 *   <li>{@code RESUMO}   — lightweight DTO returned in listings (e.g.: {@code MainItemSummaryDTO})</li>
 *   <li>{@code RESPONSE} — full DTO returned in individual lookups and write operations</li>
 *   <li>{@code CREATE}   — input DTO for creating a new record</li>
 *   <li>{@code UPDATE}   — input DTO for updating an existing record</li>
 *   <li>{@code ENTITY}   — JPA entity type (required for Envers history typing)</li>
 * </ul>
 *
 * @param <RESUMO>   summary DTO used in listings
 * @param <RESPONSE> full DTO returned in individual read/write operations
 * @param <CREATE>   creation DTO
 * @param <UPDATE>   update DTO
 * @param <ENTITY>   JPA entity type
 */
public abstract class SuperController<RESUMO, RESPONSE, CREATE, UPDATE, ENTITY> {

    /**
     * Creates a new record.
     *
     * @param dto creation data validated by Bean Validation
     * @return {@code 201 Created} with the full DTO of the created record
     */
    public abstract ResponseEntity<RESPONSE> create(CREATE dto);

    /**
     * Returns the full data of a record by its identifier.
     *
     * @param id UUID identifier of the record
     * @return {@code 200 OK} with the full DTO; {@code 404 Not Found} if it does not exist
     */
    public abstract ResponseEntity<RESPONSE> findById(UUID id);

    /**
     * Returns the summary listing of all active records.
     *
     * @return {@code 200 OK} with the list of summary DTOs
     */
    public abstract ResponseEntity<List<RESUMO>> list();

    /**
     * Updates an existing record.
     *
     * @param id  UUID identifier of the record to update
     * @param dto update data validated by Bean Validation
     * @return {@code 200 OK} with the updated full DTO; {@code 404} if it does not exist
     */
    public abstract ResponseEntity<RESPONSE> update(UUID id, UPDATE dto);

    /**
     * Performs the soft deletion of a record (sets {@code active = false}).
     *
     * @param id UUID identifier of the record to deactivate
     * @return {@code 204 In Content} after deactivation; {@code 404} if it does not exist
     */
    public abstract ResponseEntity<Void> delete(UUID id);

    /**
     * Returns all records, including logically deactivated ones.
     * Useful for administrative screens that need to view the full history.
     *
     * @return {@code 200 OK} with the complete list of summary DTOs
     */
    public abstract ResponseEntity<List<RESUMO>> findAllIncludingInactive();

    /**
     * Returns the previous revision history of a record (Hibernate Envers).
     *
     * @param id UUID identifier of the record
     * @return {@code 200 OK} with the list of revisions in chronological order;
     *         empty list if there is no history
     */
    public abstract ResponseEntity<? extends List<?>> listPreviousVersions(UUID id);
}
