package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.Category;

import java.util.List;

/**
 * JPA repository for persistence operations of {@link Category}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface CategoryRepository extends SuperRepository<Category> {

    /**
     * Returns all active categories, ordered by name in ascending order.
     *
     * @return list of active categories; never {@code null}, may be empty
     */
    List<Category> findByActiveTrueOrderByNameAsc();

    /**
     * Returns active categories whose name contains the given substring,
     * case-insensitively, ordered by name in ascending order.
     *
     * @param name name substring to search (partial, case-insensitive)
     * @return list of matching categories; never {@code null}, may be empty
     */
    List<Category> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String name);
}
