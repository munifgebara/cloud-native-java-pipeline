package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.ItemLoan;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for persistence operations of {@link ItemLoan}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface ItemLoanRepository extends SuperRepository<ItemLoan> {

    /**
     * Checks whether any loan (active or returned) exists for the given instance.
     * Used to prevent deletion of instances that already have a loan history.
     *
     * @param instanciaItemId identifier of the item instance
     * @return {@code true} if at least one loan exists for this instance
     */
    boolean existsByItemInstanceId(UUID itemInstanceId);

    /**
     * Checks whether an active loan (not yet returned) exists for the given instance.
     * An active loan has a {@code null} {@code dataDevolucao}.
     * Used to validate whether the instance can be loaned again.
     *
     * @param instanciaItemId identifier of the item instance
     * @return {@code true} if an open loan exists for this instance
     */
    boolean existsByItemInstanceIdAndReturnDateIsNull(UUID itemInstanceId);

    /**
     * Finds the active loan (not yet returned) of an item instance.
     * Returns at most one result, as an instance can only have one active loan at a time.
     *
     * @param instanciaItemId identifier of the item instance
     * @return {@link Optional} with the found active loan, or empty if there is no open loan
     */
    Optional<ItemLoan> findByItemInstanceIdAndReturnDateIsNull(UUID itemInstanceId);
}
