package br.com.munif.stella.api.mapper;

import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;


/**
 * Converte entre a entidade {@link Pessoa} e seus DTOs de entrada e saída.
 *
 * <p>Classe utilitária estática — não deve ser instanciada.
 * Centraliza toda a lógica de mapeamento de {@code Pessoa},
 * evitando duplicação nos serviços e controllers.</p>
 */
public final class PessoaMapper {

    private PessoaMapper() {
    }

    /**
     * Cria uma nova entidade {@link Pessoa} a partir dos dados de criação.
     *
     * @param dto dados de criação da pessoa; pode ser {@code null}
     * @return nova instância de {@link Pessoa} populada, ou {@code null} se {@code dto} for {@code null}
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
     * Aplica os dados de atualização sobre a entidade {@link Pessoa} existente.
     *
     * <p>O CPF/CNPJ não é atualizado por este método — é imutável após o cadastro.</p>
     *
     * @param entity entidade a ser atualizada; ignorada se {@code null}
     * @param dto    dados de atualização; ignorados se {@code null}
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
     * Converte a entidade {@link Pessoa} para o DTO completo de resposta.
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link PessoaResponseDTO} populado, ou {@code null} se {@code entity} for {@code null}
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
     * Converte a entidade {@link Pessoa} para o DTO resumido usado em listagens e seletores.
     *
     * @param entity entidade a converter; pode ser {@code null}
     * @return {@link PessoaResumoDTO} populado, ou {@code null} se {@code entity} for {@code null}
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
