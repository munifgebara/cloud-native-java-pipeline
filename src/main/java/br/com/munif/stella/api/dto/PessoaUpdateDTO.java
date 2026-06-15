package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de atualização de uma pessoa.
 *
 * <p>Permite alterar dados de contato e endereço de uma pessoa já cadastrada.
 * O CPF/CNPJ não é editável após o cadastro inicial — para corrigi-lo seria
 * necessário excluir e recadastrar a pessoa.</p>
 *
 * @param nome               nome completo ou razão social; obrigatório, até 150 caracteres
 * @param telefonePrincipal  telefone principal de contato; até 20 caracteres; opcional
 * @param telefoneSecundario telefone alternativo; até 20 caracteres; opcional
 * @param email              endereço de e-mail; até 150 caracteres; opcional
 * @param cep                CEP no formato {@code 99999-999} ou {@code 99999999}; opcional
 * @param endereco           logradouro e número; até 200 caracteres; opcional
 * @param complemento        complemento do endereço; até 100 caracteres; opcional
 * @param bairro             bairro; até 100 caracteres; opcional
 * @param cidade             cidade; até 100 caracteres; opcional
 * @param uf                 sigla do estado com 2 letras (ex.: {@code "SP"}); opcional
 */
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
