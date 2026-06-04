package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.Pessoa;

public final class EmprestimoItemMapper {

    private EmprestimoItemMapper() {
    }

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
