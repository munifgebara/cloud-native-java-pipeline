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
 * Serviço responsável pelas operações de empréstimo e devolução de instâncias de itens.
 *
 * <p>Um empréstimo marca uma instância como {@code EMPRESTADO}, desvincula-a do local atual
 * e associa-a a uma pessoa. A devolução restaura o status para {@code DISPONIVEL} e
 * define o local de retorno informado.</p>
 *
 * <p>Regras de estado da instância são validadas por {@link InstanciaItemRegras} antes
 * de qualquer alteração de dados.</p>
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
     * Registra o empréstimo de uma instância a uma pessoa.
     * A instância é desvinculada do local e tem status alterado para {@code EMPRESTADO}.
     *
     * @param dto dados do empréstimo validados pelo Bean Validation
     * @return DTO do empréstimo registrado
     * @throws IllegalArgumentException se a instância ou pessoa não existirem, se a instância
     *                                  não estiver disponível, ou se já houver empréstimo aberto
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
     * Registra a devolução de uma instância emprestada.
     * A instância é associada ao local de retorno informado e tem status restaurado para {@code DISPONIVEL}.
     *
     * @param dto dados da devolução validados pelo Bean Validation
     * @return DTO do empréstimo com a data de devolução preenchida
     * @throws IllegalArgumentException se não houver empréstimo aberto para a instância,
     *                                  se a instância não estiver emprestada, ou se o local
     *                                  de retorno não existir ou estiver inativo
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
