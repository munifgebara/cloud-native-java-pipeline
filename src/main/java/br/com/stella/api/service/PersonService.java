package br.com.stella.api.service;

import br.com.munif.common.persistencia.MRevisionEntity;
import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.PersonCreateDTO;
import br.com.stella.api.dto.PersonResponseDTO;
import br.com.stella.api.dto.PersonRevisionDTO;
import br.com.stella.api.dto.PersonSummaryDTO;
import br.com.stella.api.dto.PersonUpdateDTO;
import br.com.stella.api.entity.Person;
import br.com.stella.api.exception.DuplicateRegistrationException;
import br.com.stella.api.mapper.PersonMapper;
import br.com.stella.api.repository.PersonRepository;
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
 * Service responsible for business operations on {@link Person}.
 *
 * <p>Covers CPF/CNPJ validation, data normalization (digits only in numeric fields,
 * uppercase letters in UF, etc.) and revision history queries with identification of
 * fields changed between versions.</p>
 */
@Service
public class PersonService extends SuperService<Person, PersonRepository> {

    /**
     * Constructs the service injecting the repository and the {@code EntityManager}.
     *
     * @param repository    JPA repository for persons
     * @param entityManager entity manager used by {@code SuperService} for Envers
     */
    public PersonService(PersonRepository repository, EntityManager entityManager) {
        super(repository, entityManager, Person.class);
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
    public PersonResponseDTO criar(PersonCreateDTO dto) {
        validarCreate(dto);

        String cpfCnpjNormalizado = normalizarCpfCnpj(dto.cpfCnpj());

        if (repository.existsByTaxId(cpfCnpjNormalizado)) {
            throw new DuplicateRegistrationException("A person with this CPF/CNPJ is already registered.");
        }

        Person pessoa = PersonMapper.toEntity(dto);
        normalizarCampos(pessoa);
        pessoa.setTaxId(cpfCnpjNormalizado);

        Person salva = salvar(pessoa);
        return PersonMapper.toResponseDTO(salva);
    }

    /**
     * Returns the full DTO of a person by their identifier.
     *
     * @param id UUID of the person
     * @return full DTO of the person
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional(readOnly = true)
    public PersonResponseDTO buscarResponsePorId(UUID id) {
        return PersonMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lists all active persons in alphabetical order by name.
     *
     * @return list of summary DTOs of active persons
     */
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> listarResumo() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
                .map(PersonMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lists all persons, including inactive ones.
     *
     * @return list of summary DTOs of all persons
     */
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> listarResumoIncluindoInativos() {
        return findAllIncludingInactive().stream()
                .map(PersonMapper::toResumoDTO)
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
    public PersonResponseDTO atualizar(UUID id, PersonUpdateDTO dto) {
        validarUpdate(dto);

        Person pessoa = buscarPorId(id);
        PersonMapper.updateEntity(pessoa, dto);
        normalizarCampos(pessoa);

        Person salva = salvar(pessoa);
        return PersonMapper.toResponseDTO(salva);
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
    public List<PersonSummaryDTO> buscarPorNome(String nome) {
        String nomeTratado = BrValidations.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndNameContainingIgnoreCase(nomeTratado).stream()
                .map(PersonMapper::toResumoDTO)
                .toList();
    }

    /**
     * Returns the revision history of a person with identification of changed fields.
     *
     * <p>Uses Hibernate Envers to query revisions in descending order.
     * For each pair of consecutive revisions, compares the fields and lists those that were modified.</p>
     *
     * @param id UUID of the person
     * @return list of {@link PersonRevisionDTO} in descending revision order,
     *         each containing the fields that changed relative to the previous version
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<PersonRevisionDTO> listarRevisoes(UUID id) {
        buscarPorId(id);

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<PersonRevisionDTO> revisoes = auditReader.createQuery()
                .forRevisionsOfEntity(Person.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList()
                .stream()
                .map(this::toPessoaRevisionDTO)
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
        return repository.countByActiveTrue();
    }

    private void validarCreate(PersonCreateDTO dto) {
        String cpfCnpj = normalizarCpfCnpj(dto.cpfCnpj());

        if (cpfCnpj.length() == 11) {
            BrValidations.exigirCPFValido(cpfCnpj, "CPF");
        } else if (cpfCnpj.length() == 14) {
            BrValidations.exigirCNPJValido(cpfCnpj, "CNPJ");
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

    private void validarUpdate(PersonUpdateDTO dto) {
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
        if (BrValidations.isNotBlank(telefonePrincipal)) {
            BrValidations.exigirTelefoneValido(telefonePrincipal, "Primary phone");
        }

        if (BrValidations.isNotBlank(telefoneSecundario)) {
            BrValidations.exigirTelefoneValido(telefoneSecundario, "Secondary phone");
        }

        if (BrValidations.isNotBlank(email)) {
            BrValidations.exigirEmailValido(email, "And-mail");
        }

        if (BrValidations.isNotBlank(cep)) {
            BrValidations.exigirCEPValido(cep, "CEP");
        }

        if (BrValidations.isNotBlank(uf)) {
            String ufNormalizada = uf.trim().toUpperCase(Locale.ROOT);
            if (!ufNormalizada.matches("^[A-Z]{2}$")) {
                throw new IllegalArgumentException("Invalid state abbreviation.");
            }
        }
    }

    private String normalizarCpfCnpj(String cpfCnpj) {
        String valor = BrValidations.somenteDigitos(cpfCnpj);
        if (valor == null) {
            throw new IllegalArgumentException("CPF/CNPJ is required.");
        }
        return valor;
    }

    private void normalizarCampos(Person pessoa) {
        pessoa.setName(BrValidations.trimToNull(pessoa.getName()));
        pessoa.setPrimaryPhone(normalizarTelefone(pessoa.getPrimaryPhone()));
        pessoa.setSecondaryPhone(normalizarTelefone(pessoa.getSecondaryPhone()));
        pessoa.setEmail(normalizarEmail(pessoa.getEmail()));
        pessoa.setZipCode(normalizarCep(pessoa.getZipCode()));
        pessoa.setAddress(BrValidations.trimToNull(pessoa.getAddress()));
        pessoa.setComplement(BrValidations.trimToNull(pessoa.getComplement()));
        pessoa.setNeighborhood(BrValidations.trimToNull(pessoa.getNeighborhood()));
        pessoa.setCity(BrValidations.trimToNull(pessoa.getCity()));
        pessoa.setState(normalizarUf(pessoa.getState()));
    }

    private String normalizarTelefone(String telefone) {
        String valor = BrValidations.trimToNull(telefone);
        return valor == null ? null : BrValidations.somenteDigitos(valor);
    }

    private String normalizarEmail(String email) {
        String valor = BrValidations.trimToNull(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private String normalizarCep(String cep) {
        String valor = BrValidations.trimToNull(cep);
        return valor == null ? null : BrValidations.somenteDigitos(valor);
    }

    private String normalizarUf(String uf) {
        String valor = BrValidations.trimToNull(uf);
        return valor == null ? null : valor.toUpperCase(Locale.ROOT);
    }

    /**
     * Converts the three-element array returned by Envers into a {@link PersonRevisionDTO}.
     *
     * <p>Envers returns each revision as {@code Object[] { entity, revision, revisionType }}.
     * We use destructuring via explicit cast, as the Envers API does not offer generic typing.</p>
     *
     * @param item array {@code [Person, MRevisionEntity, RevisionType]} returned by Envers
     * @return revision DTO with changed fields still empty (populated by {@link #adicionarCamposAlterados})
     */
    private PersonRevisionDTO toPessoaRevisionDTO(Object item) {
        if (item instanceof Object[] dadosRevisao
                && dadosRevisao[0] instanceof Person pessoa
                && dadosRevisao[1] instanceof MRevisionEntity revisao
                && dadosRevisao[2] instanceof RevisionType tipo) {
            return new PersonRevisionDTO(
                    revisao.getId(),
                    revisao.getTimestamp(),
                    tipo.name(),
                    PersonMapper.toResponseDTO(pessoa),
                    List.of()
            );
        }
        throw new IllegalStateException("Unexpected Envers revision data format.");
    }

    private List<PersonRevisionDTO> adicionarCamposAlterados(List<PersonRevisionDTO> revisoes) {
        List<PersonRevisionDTO> resultado = new ArrayList<>();

        for (int i = 0; i < revisoes.size(); i++) {
            PersonRevisionDTO revisao = revisoes.get(i);
            PersonResponseDTO versaoAnterior = i + 1 < revisoes.size() ? revisoes.get(i + 1).pessoa() : null;

            resultado.add(new PersonRevisionDTO(
                    revisao.revisao(),
                    revisao.dataHora(),
                    revisao.tipo(),
                    revisao.pessoa(),
                    camposAlterados(revisao.pessoa(), versaoAnterior)
            ));
        }

        return resultado;
    }

    private List<String> camposAlterados(PersonResponseDTO atual, PersonResponseDTO anterior) {
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
