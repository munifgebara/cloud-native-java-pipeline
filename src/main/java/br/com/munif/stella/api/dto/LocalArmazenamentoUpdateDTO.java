package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de atualização de um local de armazenamento.
 *
 * <p>Permite alterar o nome, descrição, local pai e status ativo de um local já cadastrado.
 * A imagem é atualizada por endpoint separado. Informar {@code null} em {@code paiId}
 * remove a hierarquia, tornando o local um nó raiz.</p>
 *
 * @param nome      nome do local; obrigatório, até 150 caracteres
 * @param descricao descrição opcional; até 500 caracteres; opcional
 * @param paiId     identificador do novo local pai; {@code null} torna o local raiz
 * @param ativa     indica se o local está ativo; opcional
 */
public record LocalArmazenamentoUpdateDTO(
        @NotBlank(message = "Name is required.")
        @Size(max = 150, message = "Name must not exceed 150 characters.")
        String nome,

        @Size(max = 500, message = "Description must not exceed 500 characters.")
        String descricao,

        UUID paiId,

        Boolean ativa
) {}
