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

    @Transactional
    public EmprestimoItemResponseDTO registrarEmprestimo(EmprestimoItemCreateDTO dto) {
        InstanciaItem instancia = instanciaItemRepository.findById(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Instância não encontrada."));
        Pessoa pessoa = pessoaRepository.findById(dto.pessoaId())
                .orElseThrow(() -> new IllegalArgumentException("Pessoa não encontrada."));

        InstanciaItemRegras.exigirDisponivelComLocal(
                instancia,
                "Instância deve estar ativa para registrar empréstimo.",
                "Apenas instâncias disponíveis podem ser emprestadas.",
                "Instância deve possuir local atual para registrar empréstimo."
        );
        if (!pessoa.isAtivo()) {
            throw new IllegalArgumentException("Pessoa deve estar ativa para registrar empréstimo.");
        }
        if (repository.existsByInstanciaItemIdAndDataDevolucaoIsNull(instancia.getId())) {
            throw new IllegalArgumentException("Já existe empréstimo aberto para esta instância.");
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

    @Transactional
    public EmprestimoItemResponseDTO registrarDevolucao(EmprestimoItemDevolucaoDTO dto) {
        EmprestimoItem emprestimo = repository.findByInstanciaItemIdAndDataDevolucaoIsNull(dto.instanciaItemId())
                .orElseThrow(() -> new IllegalArgumentException("Não existe empréstimo aberto para esta instância."));
        InstanciaItem instancia = emprestimo.getInstanciaItem();
        LocalArmazenamento localRetorno = buscarLocalAtivo(dto.localRetornoId());

        InstanciaItemRegras.exigirEmprestada(instancia, "Instância deve estar emprestada para registrar devolução.");

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
                .orElseThrow(() -> new IllegalArgumentException("Local de retorno não encontrado."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Local de retorno deve estar ativo.");
        }
        return local;
    }
}
