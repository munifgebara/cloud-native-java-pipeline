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
