package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de criação de um item mestre.
 *
 * <p>Contém os dados fornecidos pelo usuário para registrar um novo modelo de item
 * no inventário. Campos de imagem não são incluídos aqui — a imagem é enviada
 * em endpoint dedicado após a criação.</p>
 *
 * @param nome           nome do item mestre; obrigatório, até 150 caracteres
 * @param descricao      descrição detalhada do item (características, uso, etc.); até 500 caracteres; opcional
 * @param observacoes    observações internas (histórico de manutenção, restrições, etc.); até 1000 caracteres; opcional
 * @param origemCadastro origem do cadastro (ex.: {@code "MANUAL"}, {@code "IA"}); até 50 caracteres; opcional
 * @param categoriaId    identificador da categoria do item; opcional
 * @param ativa          indica se o item deve ser criado ativo; quando {@code null}, assume {@code true}
 */
public record ItemMestreCreateDTO(
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
