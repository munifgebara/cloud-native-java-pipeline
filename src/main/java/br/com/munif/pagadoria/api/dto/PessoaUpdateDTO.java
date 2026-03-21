package br.com.munif.pagadoria.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PessoaUpdateDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 20, message = "Telefone principal deve ter no máximo 20 caracteres.")
        String telefonePrincipal,

        @Size(max = 20, message = "Telefone secundário deve ter no máximo 20 caracteres.")
        String telefoneSecundario,

        @Email(message = "E-mail inválido.")
        @Size(max = 150, message = "E-mail deve ter no máximo 150 caracteres.")
        String email,

        @Pattern(regexp = "^$|^\\d{5}-?\\d{3}$", message = "CEP deve estar no formato 99999-999 ou 99999999.")
        String cep,

        @Size(max = 200, message = "Endereço deve ter no máximo 200 caracteres.")
        String endereco,

        @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres.")
        String complemento,

        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres.")
        String bairro,

        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres.")
        String cidade,

        @Pattern(regexp = "^$|^[A-Za-z]{2}$", message = "UF deve conter 2 letras.")
        String uf
) {}