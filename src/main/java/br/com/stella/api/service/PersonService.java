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
    public PersonResponseDTO create(PersonCreateDTO dto) {
        validarCreate(dto);

        String cpfCnpjNormalizado = normalizeTaxId(dto.taxId());

        if (repository.existsByTaxId(cpfCnpjNormalizado)) {
            throw new DuplicateRegistrationException("A person with this CPF/CNPJ is already registered.");
        }

        Person person = PersonMapper.toEntity(dto);
        normalizeFields(person);
        person.setTaxId(cpfCnpjNormalizado);

        Person salva = save(person);
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
    public PersonResponseDTO findResponseById(UUID id) {
        return PersonMapper.toResponseDTO(findById(id));
    }

    /**
     * Lists all active persons in alphabetical order by name.
     *
     * @return list of summary DTOs of active persons
     */
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> listSummary() {
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
    public List<PersonSummaryDTO> listSummaryIncludingInactive() {
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
    public PersonResponseDTO update(UUID id, PersonUpdateDTO dto) {
        validarUpdate(dto);

        Person person = findById(id);
        PersonMapper.updateEntity(person, dto);
        normalizeFields(person);

        Person salva = save(person);
        return PersonMapper.toResponseDTO(salva);
    }

    /**
     * Logically deactivates a person (sets {@code active = false}).
     *
     * @param id UUID of the person to deactivate
     * @throws jakarta.persistence.EntityNotFoundException if the person does not exist
     */
    @Transactional
    public void deleteLogically(UUID id) {
        delete(id);
    }

    /**
     * Finds active persons whose name contains the given text (partial, case-insensitive search).
     *
     * @param name text to search; returns empty list if blank
     * @return list of summary DTOs of the found persons
     */
    @Transactional(readOnly = true)
    public List<PersonSummaryDTO> findByName(String name) {
        String normalizedName = BrValidations.trimToNull(name);
        if (normalizedName == null) {
            return List.of();
        }

        return repository.findByActiveTrueAndNameContainingIgnoreCase(normalizedName).stream()
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
    public List<PersonRevisionDTO> listRevisions(UUID id) {
        findById(id);

        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        List<PersonRevisionDTO> revisoes = auditReader.createQuery()
                .forRevisionsOfEntity(Person.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList()
                .stream()
                .map(this::toPersonRevisionDTO)
                .toList();

        return adicionarCamposAlterados(revisoes);
    }

    /**
     * Counts the total active persons in the registry.
     * Used by the dashboard to display the registered persons indicator.
     *
     * @return number of persons with {@code active = true}
     */
    @Transactional(readOnly = true)
    public long countActivePeople() {
        return repository.countByActiveTrue();
    }

    private void validarCreate(PersonCreateDTO dto) {
        String taxId = normalizeTaxId(dto.taxId());

        if (taxId.length() == 11) {
            BrValidations.exigirCPFValido(taxId, "CPF");
        } else if (taxId.length() == 14) {
            BrValidations.exigirCNPJValido(taxId, "CNPJ");
        } else {
            throw new IllegalArgumentException("CPF/CNPJ must contain 11 or 14 digits.");
        }

        validarCamposComuns(
                dto.primaryPhone(),
                dto.secondaryPhone(),
                dto.email(),
                dto.zipCode(),
                dto.state()
        );
    }

    private void validarUpdate(PersonUpdateDTO dto) {
        validarCamposComuns(
                dto.primaryPhone(),
                dto.secondaryPhone(),
                dto.email(),
                dto.zipCode(),
                dto.state()
        );
    }

    private void validarCamposComuns(
            String primaryPhone,
            String secondaryPhone,
            String email,
            String zipCode,
            String state
    ) {
        if (BrValidations.isNotBlank(primaryPhone)) {
            BrValidations.requireValidPhone(primaryPhone, "Primary phone");
        }

        if (BrValidations.isNotBlank(secondaryPhone)) {
            BrValidations.requireValidPhone(secondaryPhone, "Secondary phone");
        }

        if (BrValidations.isNotBlank(email)) {
            BrValidations.exigirEmailValido(email, "And-mail");
        }

        if (BrValidations.isNotBlank(zipCode)) {
            BrValidations.exigirCEPValido(zipCode, "CEP");
        }

        if (BrValidations.isNotBlank(state)) {
            String ufNormalizada = state.trim().toUpperCase(Locale.ROOT);
            if (!ufNormalizada.matches("^[A-Z]{2}$")) {
                throw new IllegalArgumentException("Invalid state abbreviation.");
            }
        }
    }

    private String normalizeTaxId(String taxId) {
        String valor = BrValidations.somenteDigitos(taxId);
        if (valor == null) {
            throw new IllegalArgumentException("CPF/CNPJ is required.");
        }
        return valor;
    }

    private void normalizeFields(Person person) {
        person.setName(BrValidations.trimToNull(person.getName()));
        person.setPrimaryPhone(normalizePhone(person.getPrimaryPhone()));
        person.setSecondaryPhone(normalizePhone(person.getSecondaryPhone()));
        person.setEmail(normalizeEmail(person.getEmail()));
        person.setZipCode(normalizeZipCode(person.getZipCode()));
        person.setAddress(BrValidations.trimToNull(person.getAddress()));
        person.setComplement(BrValidations.trimToNull(person.getComplement()));
        person.setNeighborhood(BrValidations.trimToNull(person.getNeighborhood()));
        person.setCity(BrValidations.trimToNull(person.getCity()));
        person.setState(normalizeState(person.getState()));
    }

    private String normalizePhone(String phone) {
        String valor = BrValidations.trimToNull(phone);
        return valor == null ? null : BrValidations.somenteDigitos(valor);
    }

    private String normalizeEmail(String email) {
        String valor = BrValidations.trimToNull(email);
        return valor == null ? null : valor.toLowerCase(Locale.ROOT);
    }

    private String normalizeZipCode(String zipCode) {
        String valor = BrValidations.trimToNull(zipCode);
        return valor == null ? null : BrValidations.somenteDigitos(valor);
    }

    private String normalizeState(String state) {
        String valor = BrValidations.trimToNull(state);
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
    private PersonRevisionDTO toPersonRevisionDTO(Object item) {
        if (item instanceof Object[] dadosRevisao
                && dadosRevisao[0] instanceof Person person
                && dadosRevisao[1] instanceof MRevisionEntity revisao
                && dadosRevisao[2] instanceof RevisionType tipo) {
            return new PersonRevisionDTO(
                    revisao.getId(),
                    revisao.getTimestamp(),
                    tipo.name(),
                    PersonMapper.toResponseDTO(person),
                    List.of()
            );
        }
        throw new IllegalStateException("Unexpected Envers revision data format.");
    }

    private List<PersonRevisionDTO> adicionarCamposAlterados(List<PersonRevisionDTO> revisoes) {
        List<PersonRevisionDTO> result = new ArrayList<>();

        for (int i = 0; i < revisoes.size(); i++) {
            PersonRevisionDTO revisao = revisoes.get(i);
            PersonResponseDTO versaoAnterior = i + 1 < revisoes.size() ? revisoes.get(i + 1).person() : null;

            result.add(new PersonRevisionDTO(
                    revisao.revisao(),
                    revisao.dataHora(),
                    revisao.tipo(),
                    revisao.person(),
                    camposAlterados(revisao.person(), versaoAnterior)
            ));
        }

        return result;
    }

    private List<String> camposAlterados(PersonResponseDTO atual, PersonResponseDTO anterior) {
        if (atual == null || anterior == null) {
            return List.of();
        }

        List<String> fields = new ArrayList<>();

        adicionarSeAlterado(fields, "name", atual.name(), anterior.name());
        adicionarSeAlterado(fields, "taxId", atual.taxId(), anterior.taxId());
        adicionarSeAlterado(fields, "primaryPhone", atual.primaryPhone(), anterior.primaryPhone());
        adicionarSeAlterado(fields, "secondaryPhone", atual.secondaryPhone(), anterior.secondaryPhone());
        adicionarSeAlterado(fields, "email", atual.email(), anterior.email());
        adicionarSeAlterado(fields, "zipCode", atual.zipCode(), anterior.zipCode());
        adicionarSeAlterado(fields, "address", atual.address(), anterior.address());
        adicionarSeAlterado(fields, "complement", atual.complement(), anterior.complement());
        adicionarSeAlterado(fields, "neighborhood", atual.neighborhood(), anterior.neighborhood());
        adicionarSeAlterado(fields, "city", atual.city(), anterior.city());
        adicionarSeAlterado(fields, "state", atual.state(), anterior.state());

        return fields;
    }

    private void adicionarSeAlterado(List<String> fields, String campo, Object atual, Object anterior) {
        if (!Objects.equals(atual, anterior)) {
            fields.add(campo);
        }
    }
}
