package br.com.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record MeuPerfilUpdateDTO(
        @Size(max = 100, message = "Name must not exceed 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters.")
        String lastName,

        @Email(message = "And-mail must be valid.")
        @Size(max = 150, message = "And-mail must not exceed 150 characters.")
        String email
) {}
