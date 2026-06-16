package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a person, used in selectors and cross-references.
 *
 * <p>Contains only the identifier and name, sufficient to display
 * the person in selection lists (e.g.: when registering a loan).</p>
 *
 * @param id   unique identifier of the person
 * @param nome full name or company name of the person
 */
public record PersonSummaryDTO(
        UUID id,
        String nome
) {
}
