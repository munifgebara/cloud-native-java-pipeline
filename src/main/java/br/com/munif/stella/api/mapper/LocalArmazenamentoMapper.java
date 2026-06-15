package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.LocalArmazenamentoCreateDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResponseDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoResumoDTO;
import br.com.munif.stella.api.dto.LocalArmazenamentoUpdateDTO;
import br.com.munif.stella.api.entity.LocalArmazenamento;

/**
 * Converte entre a entidade {@link LocalArmazenamento} e seus DTOs de entrada e saída.
 *
 * <p>Classe utilitária estática — não deve ser instanciada.
 * Centraliza toda a lógica de mapeamento de {@code LocalArmazenamento},
 * incluindo o cálculo do caminho hierárquico e do nível de profundidade.</p>
 */
public final class LocalArmazenamentoMapper {

    private LocalArmazenamentoMapper() {
    }

    /**
     * Cria uma nova entidade {@link LocalArmazenamento} a partir dos dados de criação.
     *
     * <p>O local pai ({@code paiId}) não é resolvido aqui — deve ser associado pelo serviço
     * antes de persistir, pois exige consulta ao repositório.</p>
     *
     * @param dto dados de criação do local; pode ser {@code null}
     * @return nova instância de {@link LocalArmazenamento} populada, ou {@code null} se {@code dto} for {@code null}
     */
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

    /**
     * Aplica os dados de atualização sobre a entidade {@link LocalArmazenamento} existente.
     *
     * <p>O local pai não é atualizado aqui — deve ser resolvido e associado pelo serviço.</p>
     *
     * @param entity entidade a ser atualizada; ignorada se {@code null}
     * @param dto    dados de atualização; ignorados se {@code null}
     */
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

    /**
     * Converte a entidade {@link LocalArmazenamento} para o DTO completo de resposta.
     *
     * <p>Calcula automaticamente o caminho hierárquico e o nível de profundidade
     * percorrendo a cadeia de locais pai.</p>
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link LocalArmazenamentoResponseDTO} populado, ou {@code null} se {@code entity} for {@code null}
     */
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

    /**
     * Converte a entidade {@link LocalArmazenamento} para o DTO resumido usado em listagens.
     *
     * <p>Recebe o caminho e o nível pré-calculados como parâmetros para evitar recalcular
     * a hierarquia em operações que já os conhecem (ex.: listagens em lote).</p>
     *
     * @param entity  entidade a converter; pode ser {@code null}
     * @param caminho caminho hierárquico pré-calculado (ex.: {@code "Prédio A > Sala 101"})
     * @param nivel   nível de profundidade pré-calculado ({@code 0} para raiz)
     * @return {@link LocalArmazenamentoResumoDTO} populado, ou {@code null} se {@code entity} for {@code null}
     */
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

    /**
     * Constrói a URL relativa para acesso à imagem do local.
     *
     * @param entity local cujo caminho de imagem será verificado
     * @return URL relativa da imagem, ou {@code null} se o local não possuir imagem
     */
    private static String imagemUrl(LocalArmazenamento entity) {
        if (entity.getImagemObjectKey() == null) {
            return null;
        }
        return "/api/public/locais/%s/imagem".formatted(entity.getId());
    }

    /**
     * Calcula o caminho completo do local percorrendo recursivamente a cadeia de pais.
     *
     * @param entity local cujo caminho será calculado
     * @return caminho no formato {@code "Pai > Filho > Neto"}, ou apenas o nome se for raiz
     */
    private static String caminho(LocalArmazenamento entity) {
        if (entity.getPai() == null) {
            return entity.getNome();
        }

        return caminho(entity.getPai()) + " > " + entity.getNome();
    }

    /**
     * Calcula o nível de profundidade do local na hierarquia.
     *
     * @param entity local cujo nível será calculado
     * @return {@code 0} para locais raiz, {@code 1} para filhos diretos, e assim por diante
     */
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
