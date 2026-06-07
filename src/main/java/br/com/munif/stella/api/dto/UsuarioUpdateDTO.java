package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UsuarioUpdateDTO(
        @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres.")
        String firstName,

        @Size(max = 100, message = "Sobrenome deve ter no máximo 100 caracteres.")
        String lastName,

        @Email(message = "E-mail deve ser válido.")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres.")
        String email,

        Boolean enabled,

        List<String> roles
) {}
