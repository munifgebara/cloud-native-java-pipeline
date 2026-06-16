package br.com.munif.stella.api.service;

import br.com.munif.comum.persistencia.MRevisionEntity;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.PessoaCreateDTO;
import br.com.munif.stella.api.dto.PessoaResponseDTO;
import br.com.munif.stella.api.dto.PessoaRevisaoDTO;
import br.com.munif.stella.api.dto.PessoaResumoDTO;
import br.com.munif.stella.api.dto.PessoaUpdateDTO;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.exception.DuplicateRegistrationException;
import br.com.munif.stella.api.mapper.PessoaMapper;
import br.com.munif.stella.api.repository.PessoaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for business operations on {@link Pessoa}.
 *
 * <p>Covers CPF/CNPJ validation, data normalization (digits only in numeric fields,
 * uppercase letters in UF, etc.) and revision history queries with identification of
 * fields changed between versions.</p>
 */
@Service
public class PessoaService extends SuperService<Pessoa, PessoaRepository> {

    /**
     * Constructs the service injecting the repository and the {@code EntityManager}.
     *
     * @param repository    JPA repository for persons
     * @param entityManager entity manager used by {@code SuperService} for Envers
     */
    public PessoaService(PessoaRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Pessoa.class);
    }

    /**
     * Registers a new person after CPF/CNPJ validation and duplicate check.
     *
     * @param dto creation data validated by Bean Validation
     * @return full DTO of the created person
     * @throws IllegalArgumentException       if the CPF/CNPJ is invalid or other optional fields
     *                                        are provided with incorrect format
     * @throws DuplicateRegistrationException if a person with the same CPF/CNPJ already exists
     */
    @Transactional
    public PessoaResponseDTO criar(PessoaCreateDTO dto) {
        validarCreate(dto);

        String cpfCnpjNormalizado = normalizarCpfCnpj(dto.cpfCnpj());

        if (repository.existsByCpfCnpj(cpfCnpjNormalizado)) {
            throw new DuplicateRegistrationException("A person with this CPF/CNPJ is already registered.");
        }

        Pessoa pessoa = PessoaMapper.toEntity(dto);
        normalizarCampos(pessoa);
        pessoa.setCpfCnpj(cpfCnpjNormalizado);

        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    /**
     * Returns the full DTO of a person by their identifier.
     *
     * @param id UUID of the person
     * @return full DTO of the person
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional(readOnly = true)
    public PessoaResponseDTO buscarResponsePorId(UUID id) {
        return PessoaMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lists all active persons in alphabetical order by name.
     *
     * @return list of summary DTOs of active persons
     */
    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all persons, including inactive ones.
     *
     * @return list of summary DTOs of all persons
     */
    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Updates the data of an existing person.
     *
     * <p>The CPF/CNPJ is not editable after the initial registration.</p>
     *
     * @param id  UUID of the person to update
     * @param dto update data validated by Bean Validation
     * @return full DTO of the updated person
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     * @throws IllegalArgumentException if phone, email, ZIP code or state abbreviation are invalid
     */
    @Transactional
    public PessoaResponseDTO atualizar(UUID id, PessoaUpdateDTO dto) {
        validarUpdate(dto);

        Pessoa pessoa = buscarPorId(id);
        PessoaMapper.updateEntity(pessoa, dto);
        normalizarCampos(pessoa);

        Pessoa salva = salvar(pessoa);
        return PessoaMapper.toResponseDTO(salva);
    }

    /**
     * Logically deactivates a person (sets {@code ativo = false}).
     *
     * @param id UUID of the person to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    /**
     * Finds active persons whose name contains the given text (partial, case-insensitive search).
     *
     * @param nome text to search; returns empty list if blank
     * @return list of summary DTOs of the found persons
     */
    @Transactional(readOnly = true)
    public List<PessoaResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCase(nomeTratado).stream()
                .map(PessoaMapper::toResumoDTO)
                .toList();
    }

    /**
     * Returns the revision history of a person with identification of changed fields.
     *
     * <p>Uses Hibernate Envers to query revisions in descending order.
     * For each pair of consecutive revisions, compares the fields and lists those that were modified.</p>
     *
     * @param id UUID of the person
     * @return list of {@link PessoaRevisaoDTO} in descending revision order,
     *         each containing the fields that changed relative to the previous version
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PessoaRevisaoDTO> listarRevisoes(UUID id) {
        buscarPorId(id);

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<PessoaRevisaoDTO> revisoes = auditReader.createQuery()
                .forRevisionsOfEntity(Pessoa.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList()
                .stream()
                .map(this::toPessoaRevisaoDTO)
                .toList();

        return adicionarCamposAlterados(revisoes);
    }

    /**
     * Counts the total active persons in the registry.
     * Used by the dashboard to display the registered persons indicator.
     *
     * @return number of persons with {@code ativo = true}
     */
    @Transactional(readOnly = true)
    public long contarPessoasAtivas() {
        return repository.countByAtivoTrue();
    }

    private void validarCreate(PessoaCreateDTO dto) {
        String cpfCnpj = normalizarCpfCnpj(dto.cpfCnpj());

        if (cpfCnpj.length() == 11) {
            ValidacoesBR.exigirCPFValido(cpfCnpj, "CPF");
        } else if (cpfCnpj.length() == 14) {
            ValidacoesBR.exigirCNPJValido(cpfCnpj, "CNPJ");
        } else {
            throw new IllegalArgumentException("CPF/CNPJ must contain 11 or 14 digits.");
        }

        validarCamposComuns(
                dto.telefonePrincipal(),
                dto.telefoneSecundario(),
                dto.email(),
                dto.cep(),
                dto.uf()
        );
    }

    private void validarUpdate(PessoaUpdateDTO dto) {
        validarCamposComuns(
                dto.telefonePrincipal(),
                dto.telefoneSecundario(),
                dto.email(),
                dto.cep(),
                dto.uf()
        );
    }

    private void validarCamposComuns(
            String telefonePrincipal,
            String telefoneSecundario,
            String email,
            String cep,
            String uf
    ) {
        if (ValidacoesBR.isNotBlank(telefonePrincipal)) {
            ValidacoesBR.exigirTelefoneValido(telefonePrincipal, "Primary phone");
        }

        if (ValidacoesBR.isNotBlank(telefoneSecundario)) {
            ValidacoesBR.exigirTelefoneValido(telefoneSecundario, "Secondary phone");
        }

        if (ValidacoesBR.isNotBlank(email)) {
            ValidacoesBR.exigirEmailValido(email, "E-mail");
        }

        if (ValidacoesBR.isNotBlank(cep)) {
            ValidacoesBR.exigirCEPValido(cep, "CEP");
        }

        if (ValidacoesBR.isNotBlank(uf)) {
            String ufNormalizada = uf.trim().toUpperCase(Locale.ROOT);
            if (!ufNormalizada.matches("^[A-Z]{2}$")) {
                throw new IllegalArgumentException("Invalid state abbreviation.");
            }
        }
    }

    private String normalizarCpfCnpj(String cpfCnpj) {
        String valor = ValidacoesBR.somenteDigitos(cpfCnpj);
        if (valor == null) {
            throw new IllegalArgumentException("CPF/CNPJ is required.");
        }
        return valor;
    }

    private void normalizarCampos(Pessoa pessoa) {
        pessoa.setNome(ValidacoesBR.trimToNull(pessoa.getNome()));
        pessoa.setTelefonePrincipal(normalizarTelefone(pessoa.getTelefonePrincipal()));
        pessoa.setTelefoneSecundario(normalizarTelefone(pessoa.getTelefoneSecundario()));
        pessoa.setEmail(normalizarEmail(pessoa.getEmail()));
        pessoa.setCep(normalizarCep(pessoa.getCep()));
        pessoa.setEndereco(ValidacoesBR.trimToNull(pessoa.getEndereco()));
        pessoa.setComplemento(ValidacoesBR.trimToNull(pessoa.getComplemento()));
        pessoa.setBairro(ValidacoesBR.trimToNull(pessoa.getBairro()));
        pessoa.setCidade(ValidacoesBR.trimToNull(pessoa.getCidade()));
        pessoa.setUf(normalizarUf(pessoa.getUf()));
    }

    private String normalizarTelefone(String telefone) {
        String valor = ValidacoesBR.trimToNull(telefone);
        return valor == null ? null : ValidacoesBR.somenteDigitos(valor);
    }

    private String normalizarEmail(String email) {
        String valor = ValidacoesBR.trimToNull(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarCep(String cep) {
        String valor = ValidacoesBR.trimToNull(cep);
        return valor == null ? null : ValidacoesBR.somenteDigitos(valor);
    }

    private String normalizarUf(String uf) {
        String valor = ValidacoesBR.trimToNull(uf);
        return valor == null ? null : valor.toUpperCase(Locale.ROOT);
    }

    /**
     * Converts the three-element array returned by Envers into a {@link PessoaRevisaoDTO}.
     *
     * <p>Envers returns each revision as {@code Object[] { entity, revision, revisionType }}.
     * We use destructuring via explicit cast, as the Envers API does not offer generic typing.</p>
     *
     * @param item array {@code [Pessoa, MRevisionEntity, RevisionType]} returned by Envers
     * @return revision DTO with changed fields still empty (populated by {@link #adicionarCamposAlterados})
     */
    private PessoaRevisaoDTO toPessoaRevisaoDTO(Object item) {
        if (item instanceof Object[] dadosRevisao
                && dadosRevisao[0] instanceof Pessoa pessoa
                && dadosRevisao[1] instanceof MRevisionEntity revisao
                && dadosRevisao[2] instanceof RevisionType tipo) {
            return new PessoaRevisaoDTO(
                    revisao.getId(),
                    revisao.getTimestamp(),
                    tipo.name(),
                    PessoaMapper.toResponseDTO(pessoa),
                    List.of()
            );
        }
        throw new IllegalStateException("Unexpected Envers revision data format.");
    }

    private List<PessoaRevisaoDTO> adicionarCamposAlterados(List<PessoaRevisaoDTO> revisoes) {
        List<PessoaRevisaoDTO> resultado = new ArrayList<>();

        for (int i = 0; i < revisoes.size(); i++) {
            PessoaRevisaoDTO revisao = revisoes.get(i);
            PessoaResponseDTO versaoAnterior = i + 1 < revisoes.size() ? revisoes.get(i + 1).pessoa() : null;

            resultado.add(new PessoaRevisaoDTO(
                    revisao.revisao(),
                    revisao.dataHora(),
                    revisao.tipo(),
                    revisao.pessoa(),
                    camposAlterados(revisao.pessoa(), versaoAnterior)
            ));
        }

        return resultado;
    }

    private List<String> camposAlterados(PessoaResponseDTO atual, PessoaResponseDTO anterior) {
        if (atual == null || anterior == null) {
            return List.of();
        }

        List<String> campos = new ArrayList<>();

        adicionarSeAlterado(campos, "nome", atual.nome(), anterior.nome());
        adicionarSeAlterado(campos, "cpfCnpj", atual.cpfCnpj(), anterior.cpfCnpj());
        adicionarSeAlterado(campos, "telefonePrincipal", atual.telefonePrincipal(), anterior.telefonePrincipal());
        adicionarSeAlterado(campos, "telefoneSecundario", atual.telefoneSecundario(), anterior.telefoneSecundario());
        adicionarSeAlterado(campos, "email", atual.email(), anterior.email());
        adicionarSeAlterado(campos, "cep", atual.cep(), anterior.cep());
        adicionarSeAlterado(campos, "endereco", atual.endereco(), anterior.endereco());
        adicionarSeAlterado(campos, "complemento", atual.complemento(), anterior.complemento());
        adicionarSeAlterado(campos, "bairro", atual.bairro(), anterior.bairro());
        adicionarSeAlterado(campos, "cidade", atual.cidade(), anterior.cidade());
        adicionarSeAlterado(campos, "uf", atual.uf(), anterior.uf());

        return campos;
    }

    private void adicionarSeAlterado(List<String> campos, String campo, Object atual, Object anterior) {
        if (!Objects.equals(atual, anterior)) {
            campos.add(campo);
        }
    }
}
