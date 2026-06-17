package br.com.stella.api.dto;

import java.util.List;
import java.util.UUID;

public record SemanticSearchItemDTO(
        UUID itemMestreId,
        String nome,
        String descricao,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        double similaridade,
        List<SemanticSearchInstanceDTO> instancias,
        List<SemanticSearchLocationDTO> locaisProvaveis
) {}
