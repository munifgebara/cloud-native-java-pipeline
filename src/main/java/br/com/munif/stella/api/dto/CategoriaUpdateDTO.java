package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de atualização de uma categoria de itens.
 *
 * <p>Contém os campos editáveis de uma categoria já cadastrada.
 * Todos os campos são substituídos integralmente — campos omitidos ({@code null})
 * limpam o valor existente, exceto o nome que é obrigatório.</p>
 *
 * @param nome      nome da categoria; obrigatório, até 150 caracteres
 * @param descricao texto explicativo sobre o escopo da categoria; até 500 caracteres; opcional
 * @param icone     chave do ícone visual (ex.: {@code "ferramentas"}); até 50 caracteres; opcional
 * @param ativa     indica se a categoria está ativa; opcional
 */
public record CategoriaUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        @Size(max = 50, message = "Icon must not exceed 50 characters.")
        String icone,

        Boolean ativa
) {}
