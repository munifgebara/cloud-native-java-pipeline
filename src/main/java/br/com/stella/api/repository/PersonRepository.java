package br.com.stella.api.repository;

import br.com.munif.common.persistencia.SuperRepository;
import br.com.stella.api.entity.Person;

/**
 * JPA repository for persistence operations of {@link Person}.
 *
 * <p>Extends {@code SuperRepository} which already provides the standard CRUD
 * and pagination methods. The methods declared here are generated automatically by
 * Spring Date JPA from the naming convention.</p>
 */
public interface PersonRepository extends SuperRepository<Person> {

    /**
     * Checks whether a person already exists with the given CPF/CNPJ.
     *
     * @param taxId CPF or CNPJ to check
     * @return {@code true} if a person exists with this document
     */
    boolean existsByTaxId(String taxId);
}
