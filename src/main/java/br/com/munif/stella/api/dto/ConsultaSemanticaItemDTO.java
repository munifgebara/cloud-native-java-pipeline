package br.com.munif.stella.api.dto;

import java.util.List;
import java.util.UUID;

public record ConsultaSemanticaItemDTO(
        UUID itemMestreId,
        String nome,
        String descricao,
        String categoriaNome,
        String categoriaIcone,
        String imagemUrl,
        double similaridade,
        List<ConsultaSemanticaInstanciaDTO> instancias,
        List<ConsultaSemanticaLocalDTO> locaisProvaveis
) {}
