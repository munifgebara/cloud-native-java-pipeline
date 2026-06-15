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
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 50, message = "Ícone deve ter no máximo 50 caracteres.")
        String icone,

        Boolean ativa
) {}
