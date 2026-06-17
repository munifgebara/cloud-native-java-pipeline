package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.StorageLocation;

import java.util.List;

/**
 * JPA repository for persistence operations of {@link StorageLocation}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface StorageLocationRepository extends SuperRepository<StorageLocation> {

    /**
     * Returns all active storage locations, ordered by name in ascending order.
     *
     * @return list of active locations; never {@code null}, may be empty
     */
    List<StorageLocation> findByActiveTrueOrderByNameAsc();

    /**
     * Returns active storage locations whose name contains the given substring,
     * case-insensitively, ordered by name in ascending order.
     *
     * @param name name substring to search (partial, case-insensitive)
     * @return list of matching locations; never {@code null}, may be empty
     */
    List<StorageLocation> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);

    /**
     * Counts the total active storage locations in the system.
     * Used for statistics displayed on the control panel.
     *
     * @return number of locations with {@code active = true}
     */
    long countByActiveTrue();
}
