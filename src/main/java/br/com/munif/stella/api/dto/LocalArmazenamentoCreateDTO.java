package br.com.munif.stella.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO de criação de um local de armazenamento.
 *
 * <p>Permite criar locais em qualquer nível da hierarquia: um local raiz (sem {@code paiId})
 * ou um sublocal (informando o {@code paiId} do local pai). A imagem é enviada
 * em endpoint separado após a criação.</p>
 *
 * @param nome      nome do local; obrigatório, até 150 caracteres
 * @param descricao descrição opcional do local (capacidade, tipo de itens, etc.); até 500 caracteres
 * @param paiId     identificador do local pai na hierarquia; {@code null} indica local raiz
 * @param ativa     indica se o local deve ser criado ativo; quando {@code null}, assume {@code true}
 */
public record LocalArmazenamentoCreateDTO(
        @NotBlank(message = "Nome é obrigatório.")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres.")
        String nome,

        @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres.")
        String descricao,

        UUID paiId,

        Boolean ativa
) {}
