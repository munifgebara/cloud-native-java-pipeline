package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;


/**
 * Converts between the {@link Pessoa} entity and its input and output DTOs.
 *
 * <p>Static utility class — must not be instantiated.
 * Centralizes all mapping logic for {@code Pessoa},
 * avoiding duplication in services and controllers.</p>
 */
public final class PessoaMapper {

    private PessoaMapper() {
    }

    /**
     * Creates a new {@link Pessoa} entity from creation data.
     *
     * @param dto person creation data; may be {@code null}
     * @return new populated {@link Pessoa} instance, or {@code null} if {@code dto} is {@code null}
     */
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

    /**
     * Applies update data onto an existing {@link Pessoa} entity.
     *
     * <p>The CPF/CNPJ is not updated by this method — it is immutable after registration.</p>
     *
     * @param entity entity to be updated; ignored if {@code null}
     * @param dto    update data; ignored if {@code null}
     */
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

    /**
     * Converts the {@link Pessoa} entity to the full response DTO.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link PessoaResponseDTO}, or {@code null} if {@code entity} is {@code null}
     */
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

    /**
     * Converts the {@link Pessoa} entity to the summary DTO used in listings and selectors.
     *
     * @param entity entity to convert; may be {@code null}
     * @return populated {@link PessoaResumoDTO}, or {@code null} if {@code entity} is {@code null}
     */
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
