package br.com.munif.stella.api.service;

import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.EmprestimoItemCreateDTO;
import br.com.munif.stella.api.dto.EmprestimoItemDevolucaoDTO;
import br.com.munif.stella.api.dto.EmprestimoItemResponseDTO;
import br.com.munif.stella.api.entity.EmprestimoItem;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.Pessoa;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.mapper.EmprestimoItemMapper;
import br.com.munif.stella.api.repository.EmprestimoItemRepository;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.PessoaRepository;
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
 * <p>Instance state rules are validated by {@link InstanciaItemRegras} before
 * any data change.</p>
 */
@Service
public class EmprestimoItemService extends SuperService<EmprestimoItem, EmprestimoItemRepository> {

    private final InstanciaItemRepository instanciaItemRepository;
    private final PessoaRepository pessoaRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;

    public EmprestimoItemService(
            EmprestimoItemRepository repository,
            EntityManager entityManager,
            InstanciaItemRepository instanciaItemRepository,
            PessoaRepository pessoaRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository
    ) {
        super(repository, entityManager, EmprestimoItem.class);
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
    public EmprestimoItemResponseDTO registrarEmprestimo(EmprestimoItemCreateDTO dto) {
        InstanciaItem instancia = instanciaItemRepository.findById(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Instance not found."));
        Pessoa pessoa = pessoaRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new IllegalArgumentException("Person not found."));

        InstanciaItemRegras.exigirDisponivelComLocal(
                instancia,
                "Instance must be active to register a loan.",
                "Only available instances can be loaned.",
                "Instance must have a current location to register a loan."
        );
        if (!pessoa.isAtivo()) {
            throw new IllegalArgumentException("Person must be active to register a loan.");
        }
        if (repository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instancia.getId())) {
            throw new IllegalArgumentException("There is already an open loan for this instance.");
        }

        instancia.setLocalAtual(null);
        instancia.setStatusOperacional(StatusOperacionalInstancia.EMPRESTADO);
        instanciaItemRepository.save(instancia);

        EmprestimoItem emprestimo = new EmprestimoItem();
        emprestimo.setInstanciaItem(instancia);
        emprestimo.setPessoa(pessoa);
        emprestimo.setPrevisaoDevolucao(dto.previsaoDevolucao());
        emprestimo.setObservacao(ValidacoesBR.trimToNull(dto.observacao()));

        return EmprestimoItemMapper.toResponseDTO(salvar(emprestimo));
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
    public EmprestimoItemResponseDTO registrarDevolucao(EmprestimoItemDevolucaoDTO dto) {
        EmprestimoItem emprestimo = repository.findByInstanciaItemIdAndDataDevolucaoIsNull(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("There is no open loan for this instance."));
        InstanciaItem instancia = emprestimo.getInstanciaItem();
        LocalArmazenamento localRetorno = buscarLocalAtivo(dto.localRetornoId());

        InstanciaItemRegras.exigirEmprestada(instancia, "Instance must be loaned to register a return.");

        emprestimo.setDataDevolucao(Instant.now());
        String observacao = ValidacoesBR.trimToNull(dto.observacao());
        if (observacao != null) {
            emprestimo.setObservacao(observacao);
        }

        instancia.setLocalAtual(localRetorno);
        instancia.setStatusOperacional(StatusOperacionalInstancia.DISPONIVEL);
        instanciaItemRepository.save(instancia);

        return EmprestimoItemMapper.toResponseDTO(salvar(emprestimo));
    }

    private LocalArmazenamento buscarLocalAtivo(java.util.UUID localId) {
        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Return location not found."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Return location must be active.");
        }
        return local;
    }
}
