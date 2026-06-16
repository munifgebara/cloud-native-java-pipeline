package br.com.munif.stella.api.dto;

import java.util.UUID;

/**
 * Full response DTO of a storage location.
 *
 * <p>Returned in create, update, and individual query operations.
 * Includes complete image metadata and hierarchy information.</p>
 *
 * @param id                 unique identifier of the location
 * @param nome               location name
 * @param descricao          description of the location; may be {@code null}
 * @param paiId              identifier of the parent location; {@code null} for root locations
 * @param paiNome            parent location name (denormalized); {@code null} for root locations
 * @param caminho            full path in the hierarchy (e.g.: {@code "Building A > Room 101"})
 * @param nivel              depth in the hierarchy: {@code 0} for root locations
 * @param imagemUrl          relative URL for accessing the image; {@code null} when no image
 * @param imagemContentType  image MIME type (e.g.: {@code "image/jpeg"}); may be {@code null}
 * @param imagemTamanhoBytes image size in bytes; may be {@code null}
 * @param ativa              indicates whether the location is active in the system
 */
public record LocalArmazenamentoResponseDTO(
        UUID id,
        String nome,
        String descricao,
        UUID paiId,
        String paiNome,
        String caminho,
        int nivel,
        String imagemUrl,
        String imagemContentType,
        Long imagemTamanhoBytes,
        boolean ativa
) {}
