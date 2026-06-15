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
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 50, message = "Ícone deve ter no máximo 50 caracteres.")
        String icone,

        Boolean ativa
) {}
