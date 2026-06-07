package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;


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
        pessoa.setTelefonePrincipal(dto.telefonePrincipal());
        pessoa.setTelefoneSecundario(dto.telefoneSecundario());
        pessoa.setEmail(dto.email());
        pessoa.setCep(dto.cep());
        pessoa.setEndereco(dto.endereco());
        pessoa.setComplemento(dto.complemento());
        pessoa.setBairro(dto.bairro());
        pessoa.setCidade(dto.cidade());
        pessoa.setUf(dto.uf());
        return pessoa;
    }

    public static void updateEntity(Pessoa entity, PessoaUpdateDTO dto) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNome(dto.nome());
        entity.setTelefonePrincipal(dto.telefonePrincipal());
        entity.setTelefoneSecundario(dto.telefoneSecundario());
        entity.setEmail(dto.email());
        entity.setCep(dto.cep());
        entity.setEndereco(dto.endereco());
        entity.setComplemento(dto.complemento());
        entity.setBairro(dto.bairro());
        entity.setCidade(dto.cidade());
        entity.setUf(dto.uf());
    }

    public static PessoaResponseDTO toResponseDTO(Pessoa entity) {
        if (entity == null) {
            return null;
        }

        return new PessoaResponseDTO(
                entity.getId(),
                entity.getNome(),
                entity.getCpfCnpj(),
                entity.getTelefonePrincipal(),
                entity.getTelefoneSecundario(),
                entity.getEmail(),
                entity.getCep(),
                entity.getEndereco(),
                entity.getComplemento(),
                entity.getBairro(),
                entity.getCidade(),
                entity.getUf(),
                entity.getCriadoEm(),
                entity.getAlteradoEm()
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
