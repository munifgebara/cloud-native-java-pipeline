package br.com.stella.api.service;

import br.com.munif.common.service.SuperService;
import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.dto.ItemLoanCreateDTO;
import br.com.stella.api.dto.ItemLoanReturnDTO;
import br.com.stella.api.dto.ItemLoanResponseDTO;
import br.com.stella.api.entity.ItemLoan;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.entity.Person;
import br.com.stella.api.entity.ItemInstanceStatus;
import br.com.stella.api.mapper.ItemLoanMapper;
import br.com.stella.api.repository.ItemLoanRepository;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.StorageLocationRepository;
import br.com.stella.api.repository.PersonRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service responsible for item instance loan and return operations.
 *
 * <p>A loan marks an instance as {@code EMPRESTADO}, unlinks it from the current location,
 * and associates it with a person. The return restores the status to {@code DISPONIVEL} and
 * sets the provided return location.</p>
 *
 * <p>Instance state rules are validated by {@link ItemInstanceRules} before
 * any data change.</p>
 */
@Service
public class ItemLoanService extends SuperService<ItemLoan, ItemLoanRepository> {

    private final ItemInstanceRepository itemInstanceRepository;
    private final PersonRepository personRepository;
    private final StorageLocationRepository storageLocationRepository;

    public ItemLoanService(
            ItemLoanRepository repository,
            EntityManager entityManager,
            ItemInstanceRepository itemInstanceRepository,
            PersonRepository personRepository,
            StorageLocationRepository storageLocationRepository
    ) {
        super(repository, entityManager, ItemLoan.class);
        this.itemInstanceRepository = itemInstanceRepository;
        this.personRepository = personRepository;
        this.storageLocationRepository = storageLocationRepository;
    }

    /**
     * Registers the loan of an instance to a person.
     * The instance is unlinked from the location and its status is changed to {@code EMPRESTADO}.
     *
     * @param dto loan data validated by Bean Validation
     * @return DTO of the registered loan
     * @throws IllegalArgumentException if the instance or person do not exist, if the instance
     *                                  is not available, or if there is already an open loan
     */
    @Transactional
    public ItemLoanResponseDTO registerLoan(ItemLoanCreateDTO dto) {
        ItemInstance instance = itemInstanceRepository.findById(dto.itemInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        Person person = personRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found."));

        ItemInstanceRules.requireAvailableWithLocation(
                instance,
                "Instance must be active to register a loan.",
                "Only available instances can be loaned.",
                "Instance must have a current location to register a loan."
        );
        if (!person.isActive()) {
            throw new IllegalArgumentException("Person must be active to register a loan.");
        }
        if (repository.existsByItemInstanceIdAndReturnDateIsNull(instance.getId())) {
            throw new IllegalArgumentException("There is already an open loan for this instance.");
        }

        instance.setCurrentLocation(null);
        instance.setOperationalStatus(ItemInstanceStatus.EMPRESTADO);
        itemInstanceRepository.save(instance);

        ItemLoan loan = new ItemLoan();
        loan.setItemInstance(instance);
        loan.setPerson(person);
        loan.setExpectedReturnDate(dto.expectedReturnDate());
        loan.setNotes(BrValidations.trimToNull(dto.notes()));

        return ItemLoanMapper.toResponseDTO(save(loan));
    }

    /**
     * Registers the return of a loaned instance.
     * The instance is associated with the provided return location and its status is restored to {@code DISPONIVEL}.
     *
     * @param dto return data validated by Bean Validation
     * @return DTO of the loan with the return date filled in
     * @throws IllegalArgumentException if there is no open loan for the instance,
     *                                  if the instance is not loaned, or if the return
     *                                  location does not exist or is inactive
     */
    @Transactional
    public ItemLoanResponseDTO registerReturn(ItemLoanReturnDTO dto) {
        ItemLoan loan = repository.findByItemInstanceIdAndReturnDateIsNull(dto.itemInstanceId())
                .orElseThrow(() -> new IllegalArgumentException("There is no open loan for this instance."));
        ItemInstance instance = loan.getItemInstance();
        StorageLocation returnLocation = findActiveLocation(dto.returnLocationId());

        ItemInstanceRules.exigirEmprestada(instance, "Instance must be loaned to register a return.");

        loan.setReturnDate(Instant.now());
        String notes = BrValidations.trimToNull(dto.notes());
        if (notes != null) {
            loan.setNotes(notes);
        }

        instance.setCurrentLocation(returnLocation);
        instance.setOperationalStatus(ItemInstanceStatus.DISPONIVEL);
        itemInstanceRepository.save(instance);

        return ItemLoanMapper.toResponseDTO(save(loan));
    }

    private StorageLocation findActiveLocation(java.util.UUID locationId) {
        StorageLocation location = storageLocationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Return location not found."));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Return location must be active.");
        }
        return location;
    }
}
