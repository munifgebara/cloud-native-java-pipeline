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

    private final ItemInstanceRepository instanciaItemRepository;
    private final PersonRepository pessoaRepository;
    private final StorageLocationRepository localArmazenamentoRepository;

    public ItemLoanService(
            ItemLoanRepository repository,
            EntityManager entityManager,
            ItemInstanceRepository instanciaItemRepository,
            PersonRepository pessoaRepository,
            StorageLocationRepository localArmazenamentoRepository
    ) {
        super(repository, entityManager, ItemLoan.class);
        this.instanciaItemRepository = instanciaItemRepository;
        this.pessoaRepository = pessoaRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
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
    public ItemLoanResponseDTO registrarEmprestimo(ItemLoanCreateDTO dto) {
        ItemInstance instance = instanciaItemRepository.findById(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        Person pessoa = pessoaRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found."));

        ItemInstanceRules.exigirDisponivelComLocal(
                instance,
                "Instance must be active to register a loan.",
                "Only available instances can be loaned.",
                "Instance must have a current location to register a loan."
        );
        if (!pessoa.isActive()) {
            throw new IllegalArgumentException("Person must be active to register a loan.");
        }
        if (repository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instance.getId())) {
            throw new IllegalArgumentException("There is already an open loan for this instance.");
        }

        instance.setCurrentLocation(null);
        instance.setOperationalStatus(ItemInstanceStatus.EMPRESTADO);
        instanciaItemRepository.save(instance);

        ItemLoan emprestimo = new ItemLoan();
        emprestimo.setItemInstance(instance);
        emprestimo.setPerson(pessoa);
        emprestimo.setExpectedReturnDate(dto.previsaoDevolucao());
        emprestimo.setNotes(BrValidations.trimToNull(dto.observacao()));

        return ItemLoanMapper.toResponseDTO(salvar(emprestimo));
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
    public ItemLoanResponseDTO registrarDevolucao(ItemLoanReturnDTO dto) {
        ItemLoan emprestimo = repository.findByInstanciaItemIdAndDataDevolucaoIsNull(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("There is no open loan for this instance."));
        ItemInstance instance = emprestimo.getItemInstance();
        StorageLocation localRetorno = buscarLocalAtivo(dto.localRetornoId());

        ItemInstanceRules.exigirEmprestada(instance, "Instance must be loaned to register a return.");

        emprestimo.setReturnDate(Instant.now());
        String observacao = BrValidations.trimToNull(dto.observacao());
        if (observacao != null) {
            emprestimo.setNotes(observacao);
        }

        instance.setCurrentLocation(localRetorno);
        instance.setOperationalStatus(ItemInstanceStatus.DISPONIVEL);
        instanciaItemRepository.save(instance);

        return ItemLoanMapper.toResponseDTO(salvar(emprestimo));
    }

    private StorageLocation buscarLocalAtivo(java.util.UUID localId) {
        StorageLocation location = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Return location not found."));
        if (!location.isActive()) {
            throw new IllegalArgumentException("Return location must be active.");
        }
        return location;
    }
}
