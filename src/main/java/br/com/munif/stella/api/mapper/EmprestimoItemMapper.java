package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.Pessoa;

/**
 * Converte entre a entidade {@link EmprestimoItem} e seus DTOs de saída.
 *
 * <p>Classe utilitária estática — não deve ser instanciada.
 * Como empréstimos são gerenciados por fluxos específicos (criação, devolução),
 * este mapper expõe apenas a conversão para resposta.</p>
 */
public final class EmprestimoItemMapper {

    private EmprestimoItemMapper() {
    }

    /**
     * Converte a entidade {@link EmprestimoItem} para o DTO completo de resposta.
     *
     * <p>Inclui os dados desnormalizados da instância e da pessoa para evitar
     * requisições adicionais no cliente.</p>
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link EmprestimoItemResponseDTO} populado, ou {@code null} se {@code entity} for {@code null}
     */
    public static EmprestimoItemResponseDTO toResponseDTO(EmprestimoItem entity) {
        if (entity == null) {
            return null;
        }

        InstanciaItem instancia = entity.getInstanciaItem();
        Pessoa pessoa = entity.getPessoa();
        return new EmprestimoItemResponseDTO(
                entity.getId(),
                instancia == null ? null : instancia.getId(),
                identificacao(instancia),
                pessoa == null ? null : pessoa.getId(),
                pessoa == null ? null : pessoa.getNome(),
                entity.getDataEmprestimo(),
                entity.getPrevisaoDevolucao(),
                entity.getDataDevolucao(),
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
