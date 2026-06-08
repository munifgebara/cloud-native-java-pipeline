package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;

public final class LocalArmazenamentoMapper {

    private LocalArmazenamentoMapper() {
    }

    public static LocalArmazenamento toEntity(LocalArmazenamentoCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        LocalArmazenamento local = new LocalArmazenamento();
        local.setNome(dto.nome());
        local.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            local.setAtivo(dto.ativa());
        }
        return local;
    }

    public static void updateEntity(LocalArmazenamento entity, LocalArmazenamentoUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setDescricao(dto.descricao());
        if (dto.ativa() != null) {
            entity.setAtivo(dto.ativa());
        }
    }

    public static LocalArmazenamentoResponseDTO toResponseDTO(LocalArmazenamento entity) {
        if (entity == null) {
            return null;
        }

        return new LocalArmazenamentoResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getPai() == null ? null : entity.getPai().getId(),
                entity.getPai() == null ? null : entity.getPai().getNome(),
                caminho(entity),
                nivel(entity),
                imagemUrl(entity),
                entity.getImagemContentType(),
                entity.getImagemTamanhoBytes(),
                entity.isAtivo()
        );
    }

    public static LocalArmazenamentoResumoDTO toResumoDTO(LocalArmazenamento entity, String caminho, int nivel) {
        if (entity == null) {
            return null;
        }

        return new LocalArmazenamentoResumoDTO(
                entity.getId(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getPai() == null ? null : entity.getPai().getId(),
                entity.getPai() == null ? null : entity.getPai().getNome(),
                caminho,
                nivel,
                imagemUrl(entity),
                entity.isAtivo()
        );
    }

    private static String imagemUrl(LocalArmazenamento entity) {
        if (entity.getImagemObjectKey() == null) {
            return null;
        }
        return "/api/public/locais/%s/imagem".formatted(entity.getId());
    }

    private static String caminho(LocalArmazenamento entity) {
        if (entity.getPai() == null) {
            return entity.getNome();
        }

        return caminho(entity.getPai()) + " > " + entity.getNome();
    }

    private static int nivel(LocalArmazenamento entity) {
        int nivel = 0;
        LocalArmazenamento atual = entity.getPai();
        while (atual != null) {
            nivel++;
            atual = atual.getPai();
        }
        return nivel;
    }
}
