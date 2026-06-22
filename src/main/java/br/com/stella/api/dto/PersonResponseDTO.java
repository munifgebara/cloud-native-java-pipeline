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
 * @param name               full name or company name
 * @param taxId            CPF or CNPJ (digits only)
 * @param primaryPhone  primary contact phone; may be {@code null}
 * @param secondaryPhone alternative phone; may be {@code null}
 * @param email              email address; may be {@code null}
 * @param zipCode                ZIP code (digits only); may be {@code null}
 * @param address           street and number; may be {@code null}
 * @param complement        address complement; may be {@code null}
 * @param neighborhood             neighbourhood; may be {@code null}
 * @param city             city; may be {@code null}
 * @param state                 state abbreviation (2 letters); may be {@code null}
 * @param photoUrl              relative URL for accessing the photo; {@code null} when no photo
 * @param photoContentType      photo MIME type; may be {@code null}
 * @param photoSizeBytes        photo size in bytes; may be {@code null}
 * @param createdAt           record creation date and time (UTC)
 * @param updatedAt         date and time of the last change to the record (UTC)
 */
public record PersonResponseDTO(
        UUID id,
        String name,
        String taxId,
        String primaryPhone,
        String secondaryPhone,
        String email,
        String zipCode,
        String address,
        String complement,
        String neighborhood,
        String city,
        String state,
        String photoUrl,
        String photoContentType,
        Long photoSizeBytes,
        Instant createdAt,
        Instant updatedAt
) {}
