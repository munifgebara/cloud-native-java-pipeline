package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de atualização de um item mestre.
 *
 * <p>Contém os campos editáveis de um item mestre já cadastrado.
 * Campos de imagem não são atualizados por este DTO — utilize o endpoint
 * de upload de imagem para substituir a imagem do item.</p>
 *
 * @param nome           nome do item mestre; obrigatório, até 150 caracteres
 * @param descricao      descrição detalhada do item; até 500 caracteres; opcional
 * @param observacoes    observações internas; até 1000 caracteres; opcional
 * @param origemCadastro origem do cadastro; até 50 caracteres; opcional
 * @param categoriaId    identificador da categoria; {@code null} remove a associação com a categoria
 * @param ativa          indica se o item está ativo; opcional
 */
public record ItemMestreUpdateDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String descricao,

        @Size(max = 1000, message = "Observações devem ter no máximo 1000 caracteres.")
        String observacoes,

        @Size(max = 50, message = "Origem do cadastro deve ter no máximo 50 caracteres.")
        String origemCadastro,

        UUID categoriaId,

        Boolean ativa
) {}
