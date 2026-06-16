package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioUpdateDTO(
        @Size(max = 100, message = "Name must not exceed 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters.")
        String lastName,

        @Email(message = "E-mail must be valid.")
        @Size(max = 150, message = "E-mail must not exceed 150 characters.")
        String email,

        Boolean enabled,

        List<String> roles
) {}
