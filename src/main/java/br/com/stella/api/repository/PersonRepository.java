package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.Person;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository for persistence operations of {@link Person}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface PersonRepository extends SuperRepository<Person> {

    /**
     * Finds a person by exact CPF or CNPJ.
     * Used to check for duplicates before registering and to authenticate
     * searches by document.
     *
     * @param cpfCnpj CPF (11 digits) or CNPJ (14 digits) without formatting
     * @return {@link Optional} with the found person, or empty if not found
     */
    Optional<Person> findByCpfCnpj(String cpfCnpj);

    /**
     * Checks whether a person already exists with the given CPF/CNPJ.
     * More efficient than {@link #findByCpfCnpj} when only existence needs to be checked.
     *
     * @param cpfCnpj CPF or CNPJ to check
     * @return {@code true} if a person exists with this document
     */
    boolean existsByCpfCnpj(String cpfCnpj);

    /**
     * Finds active persons whose name contains the given substring,
     * case-insensitively.
     *
     * @param nome name substring to search (partial, case-insensitive)
     * @return list of matching persons; never {@code null}, may be empty
     */
    List<Person> findByAtivoTrueAndNomeContainingIgnoreCase(String nome);

    /**
     * Returns all active persons, ordered by name in ascending order.
     *
     * @return list of active persons; never {@code null}, may be empty
     */
    List<Person> findByActiveTrueOrderByNameAsc();

    /**
     * Counts the total active persons in the system.
     * Used for statistics displayed on the control panel.
     *
     * @return number of persons with {@code ativo = true}
     */
    long countByAtivoTrue();
}
