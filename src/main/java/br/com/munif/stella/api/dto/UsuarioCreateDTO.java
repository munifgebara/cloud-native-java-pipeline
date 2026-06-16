package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioCreateDTO(
        @NotBlank(message = "Username is required.")
        @Size(max = 100, message = "Username must not exceed 100 characters.")
        String username,

        @Size(max = 100, message = "Name must not exceed 100 characters.")
        String firstName,

        @Size(max = 100, message = "Last name must not exceed 100 characters.")
        String lastName,

        @Email(message = "E-mail must be valid.")
        @Size(max = 150, message = "E-mail must not exceed 150 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters.")
        String password,

        Boolean enabled,

        List<String> roles
) {}
