package br.com.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlterarSenhaDTO(
        @NotBlank(message = "Current password is required.")
        String senhaAtual,

        @NotBlank(message = "New password is required.")
        @Size(min = 6, max = 100, message = "New password must be between 6 and 100 characters.")
        String novaSenha
) {}
