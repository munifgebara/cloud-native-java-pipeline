package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * Summary DTO of a storage location, used in listings.
 *
 * <p>Includes hierarchy information (parent, path and level) to facilitate
 * display in tree structures and hierarchical lists in the interface.</p>
 *
 * @param id        unique identifier of the location
 * @param nome      location name
 * @param descricao description of the location; may be {@code null}
 * @param paiId     identifier of the parent location; {@code null} for root locations
 * @param paiNome   parent location name (denormalized); {@code null} for root locations
 * @param caminho   full path of the location in the hierarchy (e.g.: {@code "Building A > Room 101 > Cabinet 2"})
 * @param nivel     depth in the hierarchy: {@code 0} for root locations, {@code 1} for children, etc.
 * @param imagemUrl relative URL for accessing the location image; {@code null} when no image
 * @param ativa     indicates whether the location is active in the system
 */
public record LocalArmazenamentoResumoDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        boolean ativa
) {}
