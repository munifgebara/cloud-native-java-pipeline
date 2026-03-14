package br.com.munif.pagadoria.api.mapper;

import br.com.munif.pagadoria.api.dto.PessoaCreateDTO;
import br.com.munif.pagadoria.api.dto.PessoaResponseDTO;
import br.com.munif.pagadoria.api.dto.PessoaResumoDTO;
import br.com.munif.pagadoria.api.dto.PessoaUpdateDTO;
import br.com.munif.pagadoria.api.entity.Pessoa;

public final class PessoaMapper {

    private PessoaMapper() {
    }

    public static Pessoa toEntity(PessoaCreateDTO dto) {
        if (dto == null) {
            return null;
        }

        Pessoa pessoa = new Pessoa();
        pessoa.setNome(dto.nome());
        pessoa.setCpfCnpj(dto.cpfCnpj());
        return pessoa;
    }

    public static void updateEntity(Pessoa entity, PessoaUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
    }

    public static PessoaResponseDTO toResponseDTO(Pessoa entity) {
        if (entity == null) {
            return null;
        }

        return new PessoaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getCpfCnpj()
        );
    }

    public static PessoaResumoDTO toResumoDTO(Pessoa entity) {
        if (entity == null) {
            return null;
        }

        return new PessoaResumoDTO(
                entity.getId(),
                entity.getNome()
        );
    }
}
