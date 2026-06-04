package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.mapper.InstanciaItemMapper;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InstanciaItemService extends SuperService<InstanciaItem, InstanciaItemRepository> {

    private final ItemMestreRepository itemMestreRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;

    public InstanciaItemService(
            InstanciaItemRepository repository,
            EntityManager entityManager,
            ItemMestreRepository itemMestreRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository
    ) {
        super(repository, entityManager, InstanciaItem.class);
        this.itemMestreRepository = itemMestreRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
    }

    @Transactional
    public InstanciaItemResponseDTO criar(InstanciaItemCreateDTO dto) {
        InstanciaItem instancia = InstanciaItemMapper.toEntity(dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(buscarItemMestreAtivo(dto.itemMestreId()));
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));

        InstanciaItem salva = salvar(instancia);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setAtivo(false);
            salva = salvar(salva);
        }
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    @Transactional(readOnly = true)
    public InstanciaItemResponseDTO buscarResponsePorId(UUID id) {
        return InstanciaItemMapper.toResponseDTO(buscarPorId(id));
    }

    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> buscarPorIdentificador(String identificador) {
        String valor = ValidacoesBR.trimToNull(identificador);
        if (valor == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndIdentificadorContainingIgnoreCaseOrderByIdentificadorAsc(valor).stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> filtrar(String identificacao, String itemMestre, UUID categoriaId, StatusOperacionalInstancia statusOperacional) {
        return repository.filtrarAtivas(
                        ValidacoesBR.trimToNull(identificacao),
                        ValidacoesBR.trimToNull(itemMestre),
                        categoriaId,
                        statusOperacional
                ).stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    @Transactional
    public InstanciaItemResponseDTO atualizar(UUID id, InstanciaItemUpdateDTO dto) {
        InstanciaItem instancia = buscarPorId(id);
        ItemMestre itemMestre = buscarItemMestreAtivo(dto.itemMestreId());

        InstanciaItemMapper.updateEntity(instancia, dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(itemMestre);
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));

        InstanciaItem salva = salvar(instancia);
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        excluir(id);
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<InstanciaItem>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
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
        if (localId == null) {
            return null;
        }

        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Local atual não encontrado."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Local atual deve estar ativo.");
        }
        return local;
    }

    private void normalizarCampos(InstanciaItem instancia) {
        instancia.setIdentificador(ValidacoesBR.trimToNull(instancia.getIdentificador()));
        instancia.setPatrimonio(ValidacoesBR.trimToNull(instancia.getPatrimonio()));
        instancia.setNumeroSerie(ValidacoesBR.trimToNull(instancia.getNumeroSerie()));
        instancia.setObservacoes(ValidacoesBR.trimToNull(instancia.getObservacoes()));
    }

    private void validarIdentificacao(InstanciaItem instancia) {
        if (instancia.getIdentificador() == null && instancia.getPatrimonio() == null && instancia.getNumeroSerie() == null) {
            throw new IllegalArgumentException("Informe identificador, patrimônio ou número de série da instância.");
        }
    }
}
