package br.com.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a main item, used in listings and selectors.
 *
 * <p>Contains only the most relevant fields for display in grids and lists,
 * including the image URL for thumbnail viewing.</p>
 *
 * @param id             unique identifier of the main item
 * @param nome           main item name
 * @param descricao      brief description of the item; may be {@code null}
 * @param categoriaId    identifier of the associated category; may be {@code null}
 * @param categoriaNome  category name (denormalized); may be {@code null}
 * @param categoriaIcone category icon key; may be {@code null}
 * @param imagemUrl      relative URL for accessing the item image; {@code null} when no image
 * @param ativa          indicates whether the item is active in the system
 */
public record MainItemSummaryDTO(
        UUID id,
        String nome,
        String descricao,
        UUID categoriaId,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        boolean ativa
) {}
