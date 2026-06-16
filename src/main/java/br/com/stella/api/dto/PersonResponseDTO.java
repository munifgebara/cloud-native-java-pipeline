package br.com.stella.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Full response DTO of a person.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes all registration data of the person, including full address
 * and audit timestamps.</p>
 *
 * @param id                 unique identifier of the person
 * @param nome               full name or company name
 * @param cpfCnpj            CPF or CNPJ (digits only)
 * @param telefonePrincipal  primary contact phone; may be {@code null}
 * @param telefoneSecundario alternative phone; may be {@code null}
 * @param email              email address; may be {@code null}
 * @param cep                ZIP code (digits only); may be {@code null}
 * @param endereco           street and number; may be {@code null}
 * @param complemento        address complement; may be {@code null}
 * @param bairro             neighbourhood; may be {@code null}
 * @param cidade             city; may be {@code null}
 * @param uf                 state abbreviation (2 letters); may be {@code null}
 * @param criadoEm           record creation date and time (UTC)
 * @param alteradoEm         date and time of the last change to the record (UTC)
 */
public record PersonResponseDTO(
        UUID id,
        String nome,
        String cpfCnpj,
        String telefonePrincipal,
        String telefoneSecundario,
        String email,
        String cep,
        String endereco,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        Instant criadoEm,
        Instant alteradoEm
) {}
