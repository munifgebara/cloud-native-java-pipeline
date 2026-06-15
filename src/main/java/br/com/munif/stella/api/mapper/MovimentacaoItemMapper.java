package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;

/**
 * Converte entre a entidade {@link MovimentacaoItem} e seus DTOs de saída.
 *
 * <p>Classe utilitária estática — não deve ser instanciada.
 * Como movimentações não possuem DTO de criação próprio (são criadas pelos serviços
 * de forma interna), este mapper expõe apenas a conversão para resposta.</p>
 */
public final class MovimentacaoItemMapper {

    private MovimentacaoItemMapper() {
    }

    /**
     * Converte a entidade {@link MovimentacaoItem} para o DTO completo de resposta.
     *
     * <p>Inclui os dados desnormalizados de instância, local de origem e local de destino
     * para evitar requisições adicionais no cliente.</p>
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link MovimentacaoItemResponseDTO} populado, ou {@code null} se {@code entity} for {@code null}
     */
    public static MovimentacaoItemResponseDTO toResponseDTO(MovimentacaoItem entity) {
        if (entity == null) {
            return null;
        }

        InstanciaItem instancia = entity.getInstanciaItem();
        LocalArmazenamento localOrigem = entity.getLocalOrigem();
        LocalArmazenamento localDestino = entity.getLocalDestino();
        return new MovimentacaoItemResponseDTO(
                entity.getId(),
                entity.getTipo(),
                entity.getDataMovimentacao(),
                instancia == null ? null : instancia.getId(),
                identificacao(instancia),
                localOrigem == null ? null : localOrigem.getId(),
                localOrigem == null ? null : localOrigem.getNome(),
                localDestino == null ? null : localDestino.getId(),
                localDestino == null ? null : localDestino.getNome(),
                entity.getMotivo(),
                entity.getObservacao()
        );
    }

    /**
     * Retorna a identificação legível da instância, priorizando:
     * identificador interno, patrimônio e, por último, número de série.
     *
     * @param instancia instância cujo identificador será resolvido; pode ser {@code null}
     * @return primeiro campo de identificação não nulo, ou {@code null} se a instância for {@code null}
     */
    private static String identificacao(InstanciaItem instancia) {
        if (instancia == null) {
            return null;
        }
        if (instancia.getIdentificador() != null) {
            return instancia.getIdentificador();
        }
        if (instancia.getPatrimonio() != null) {
            return instancia.getPatrimonio();
        }
        return instancia.getNumeroSerie();
    }
}
