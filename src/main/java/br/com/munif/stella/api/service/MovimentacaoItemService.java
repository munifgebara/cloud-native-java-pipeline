package br.com.munif.stella.api.service;

import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.MovimentacaoEntradaCreateDTO;
import br.com.munif.stella.api.dto.MovimentacaoItemResponseDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.MovimentacaoItem;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.entity.TipoMovimentacaoItem;
import br.com.munif.stella.api.mapper.MovimentacaoItemMapper;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.MovimentacaoItemRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MovimentacaoItemService extends SuperService<MovimentacaoItem, MovimentacaoItemRepository> {

    private final InstanciaItemRepository instanciaItemRepository;
    private final ItemMestreRepository itemMestreRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;

    public MovimentacaoItemService(
            MovimentacaoItemRepository repository,
            EntityManager entityManager,
            InstanciaItemRepository instanciaItemRepository,
            ItemMestreRepository itemMestreRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository
    ) {
        super(repository, entityManager, MovimentacaoItem.class);
        this.instanciaItemRepository = instanciaItemRepository;
        this.itemMestreRepository = itemMestreRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
    }

    @Transactional
    public MovimentacaoItemResponseDTO registrarEntrada(MovimentacaoEntradaCreateDTO dto) {
        InstanciaItem instancia = new InstanciaItem();
        instancia.setItemMestre(buscarItemMestreAtivo(dto.itemMestreId()));
        instancia.setLocalAtual(buscarLocalAtivo(dto.localDestinoId()));
        instancia.setIdentificador(ValidacoesBR.trimToNull(dto.identificador()));
        instancia.setPatrimonio(ValidacoesBR.trimToNull(dto.patrimonio()));
        instancia.setNumeroSerie(ValidacoesBR.trimToNull(dto.numeroSerie()));
        instancia.setObservacoes(ValidacoesBR.trimToNull(dto.observacao()));
        instancia.setStatusOperacional(StatusOperacionalInstancia.DISPONIVEL);

        validarIdentificacao(instancia);

        InstanciaItem instanciaSalva = instanciaItemRepository.save(instancia);

        MovimentacaoItem movimentacao = new MovimentacaoItem();
        movimentacao.setTipo(TipoMovimentacaoItem.ENTRADA);
        movimentacao.setInstanciaItem(instanciaSalva);
        movimentacao.setLocalDestino(instanciaSalva.getLocalAtual());
        movimentacao.setObservacao(ValidacoesBR.trimToNull(dto.observacao()));

        return MovimentacaoItemMapper.toResponseDTO(salvar(movimentacao));
    }

    private ItemMestre buscarItemMestreAtivo(UUID itemMestreId) {
        ItemMestre itemMestre = itemMestreRepository.findById(itemMestreId)
                .orElseThrow(() -> new IllegalArgumentException("Item mestre não encontrado."));
        if (!itemMestre.isAtivo()) {
            throw new IllegalArgumentException("Item mestre deve estar ativo.");
        }
        return itemMestre;
    }

    private LocalArmazenamento buscarLocalAtivo(UUID localId) {
        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Local destino não encontrado."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Local destino deve estar ativo.");
        }
        return local;
    }

    private void validarIdentificacao(InstanciaItem instancia) {
        if (instancia.getIdentificador() == null && instancia.getPatrimonio() == null && instancia.getNumeroSerie() == null) {
            throw new IllegalArgumentException("Informe identificador, patrimônio ou número de série da instância.");
        }
    }
}
