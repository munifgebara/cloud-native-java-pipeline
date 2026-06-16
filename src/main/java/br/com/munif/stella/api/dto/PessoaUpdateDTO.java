package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Update DTO for a person.
 *
 * <p>Allows changing contact and address data for an already registered person.
 * The CPF/CNPJ is not editable after the initial registration — to correct it
 * the person would need to be deleted and re-registered.</p>
 *
 * @param nome               full name or company name; required, up to 150 characters
 * @param telefonePrincipal  primary contact phone; up to 20 characters; optional
 * @param telefoneSecundario alternative phone; up to 20 characters; optional
 * @param email              email address; up to 150 characters; optional
 * @param cep                ZIP code in format {@code 99999-999} or {@code 99999999}; optional
 * @param endereco           street and number; up to 200 characters; optional
 * @param complemento        address complement; up to 100 characters; optional
 * @param bairro             neighbourhood; up to 100 characters; optional
 * @param cidade             city; up to 100 characters; optional
 * @param uf                 state abbreviation with 2 letters (e.g.: {@code "SP"}); optional
 */
public record PessoaUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 20, message = "Primary phone must not exceed 20 characters.")
        String telefonePrincipal,

        @Size(max = 20, message = "Secondary phone must not exceed 20 characters.")
        String telefoneSecundario,

        @Email(message = "Invalid e-mail.")
        @Size(max = 150, message = "E-mail must not exceed 150 characters.")
        String email,

        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "ZIP code must be in the format 99999-999 or 99999999.")
        String cep,

        @Size(max = 200, message = "Address must not exceed 200 characters.")
        String endereco,

        @Size(max = 100, message = "Complement must not exceed 100 characters.")
        String complemento,

        @Size(max = 100, message = "Neighbourhood must not exceed 100 characters.")
        String bairro,

        @Size(max = 100, message = "City must not exceed 100 characters.")
        String cidade,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "State abbreviation must contain 2 letters.")
        String uf
) {}
