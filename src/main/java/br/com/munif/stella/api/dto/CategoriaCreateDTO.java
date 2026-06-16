package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de criação de uma categoria de itens.
 *
 * <p>Contém os dados necessários para registrar uma nova categoria no sistema.
 * O ícone deve ser uma chave válida conforme definido por {@code CategoriaIcone}.</p>
 *
 * @param nome     nome da categoria; obrigatório, até 150 caracteres
 * @param descricao texto explicativo sobre o escopo da categoria; até 500 caracteres; opcional
 * @param icone    chave do ícone visual da categoria (ex.: {@code "eletronicos"}); até 50 caracteres; opcional
 * @param ativa    indica se a categoria deve ser criada ativa; quando {@code null}, assume {@code true}
 */
public record CategoriaCreateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        @Size(max = 50, message = "Icon must not exceed 50 characters.")
        String icone,

        Boolean ativa
) {}
