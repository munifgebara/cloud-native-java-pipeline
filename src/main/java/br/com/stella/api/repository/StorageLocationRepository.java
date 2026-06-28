package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.StorageLocation;

/**
 * JPA repository for persistence operations of {@link StorageLocation}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface StorageLocationRepository extends SuperRepository<StorageLocation> {
}
