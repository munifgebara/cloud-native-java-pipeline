package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.InstanciaItemCreateDTO;
import br.com.munif.stella.api.dto.InstanciaItemHistoricoDTO;
import br.com.munif.stella.api.dto.InstanciaItemResponseDTO;
import br.com.munif.stella.api.dto.InstanciaItemResumoDTO;
import br.com.munif.stella.api.dto.InstanciaItemUpdateDTO;
import br.com.munif.stella.api.entity.InstanciaItem;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.entity.LocalArmazenamento;
import br.com.munif.stella.api.entity.StatusOperacionalInstancia;
import br.com.munif.stella.api.mapper.InstanciaItemMapper;
import br.com.munif.stella.api.mapper.MovimentacaoItemMapper;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import br.com.munif.stella.api.repository.EmprestimoItemRepository;
import br.com.munif.stella.api.repository.InstanciaItemRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import br.com.munif.stella.api.repository.LocalArmazenamentoRepository;
import br.com.munif.stella.api.repository.MovimentacaoItemRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço responsável pelas operações de negócio sobre {@link InstanciaItem}.
 *
 * <p>Gerencia o ciclo de vida das instâncias físicas de itens de inventário:
 * criação via entrada, atualização de local e status, exclusão lógica e consulta
 * de histórico de movimentações.</p>
 *
 * <p>Regras de negócio sobre a consistência entre status e localização são
 * delegadas à classe {@link InstanciaItemRegras}.</p>
 */
@Service
public class InstanciaItemService extends SuperService<InstanciaItem, InstanciaItemRepository> {

    private static final Logger log = LoggerFactory.getLogger(InstanciaItemService.class);

    private final ItemMestreRepository itemMestreRepository;
    private final LocalArmazenamentoRepository localArmazenamentoRepository;
    private final MovimentacaoItemRepository movimentacaoItemRepository;
    private final EmprestimoItemRepository emprestimoItemRepository;

    public InstanciaItemService(
            InstanciaItemRepository repository,
            EntityManager entityManager,
            ItemMestreRepository itemMestreRepository,
            LocalArmazenamentoRepository localArmazenamentoRepository,
            MovimentacaoItemRepository movimentacaoItemRepository,
            EmprestimoItemRepository emprestimoItemRepository
    ) {
        super(repository, entityManager, InstanciaItem.class);
        this.itemMestreRepository = itemMestreRepository;
        this.localArmazenamentoRepository = localArmazenamentoRepository;
        this.movimentacaoItemRepository = movimentacaoItemRepository;
        this.emprestimoItemRepository = emprestimoItemRepository;
    }

    /**
     * Cria uma nova instância de item no inventário.
     *
     * @param dto dados de criação validados pelo Bean Validation
     * @return DTO completo da instância criada
     * @throws IllegalArgumentException se o item mestre ou local não existirem, estiverem inativos,
     *                                  se nenhum identificador for informado, ou se o status e
     *                                  o local forem incompatíveis
     */
    @Transactional
    public InstanciaItemResponseDTO criar(InstanciaItemCreateDTO dto) {
        InstanciaItem instancia = InstanciaItemMapper.toEntity(dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(buscarItemMestreAtivo(dto.itemMestreId()));
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));
        InstanciaItemRegras.validarCoerenciaStatusLocal(instancia);

        InstanciaItem salva = salvar(instancia);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salva.setAtivo(false);
            salva = salvar(salva);
        }
        StructuredBusinessLogger.info(log, "inventory", "instance-created", StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getItemMestre() == null ? null : salva.getItemMestre().getId(),
                "location_id", salva.getLocalAtual() == null ? null : salva.getLocalAtual().getId(),
                "success", true
        ));
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    /**
     * Retorna o DTO completo de uma instância pelo seu identificador.
     *
     * @param id UUID da instância
     * @return DTO completo da instância
     * @throws jakarta.persistence.EntityNotFoundException se a instância não existir
     */
    @Transactional(readOnly = true)
    public InstanciaItemResponseDTO buscarResponsePorId(UUID id) {
        return InstanciaItemMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Retorna o histórico completo de movimentações de uma instância.
     *
     * @param id UUID da instância
     * @return DTO com a instância e sua lista de movimentações em ordem cronológica
     */
    @Transactional(readOnly = true)
    public InstanciaItemHistoricoDTO buscarHistorico(UUID id) {
        InstanciaItem instancia = buscarPorId(id);
        var movimentacoes = movimentacaoItemRepository.findByInstanciaItemIdOrderByDataMovimentacaoAscCriadoEmAsc(id).stream()
                .map(MovimentacaoItemMapper::toResponseDTO)
                .toList();

        return new InstanciaItemHistoricoDTO(
                InstanciaItemMapper.toResponseDTO(instancia),
                movimentacoes
        );
    }

    /**
     * Lista todas as instâncias ativas ordenadas por identificador, patrimônio e número de série.
     *
     * @return lista de DTOs de resumo das instâncias ativas
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByIdentificadorAscPatrimonioAscNumeroSerieAsc().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lista todas as instâncias, incluindo as inativadas logicamente.
     *
     * @return lista de DTOs de resumo de todas as instâncias
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Busca instâncias ativas cujo campo {@code identificador} contenha o texto informado.
     *
     * @param identificador texto a buscar; retorna lista vazia se em branco
     * @return lista de DTOs de resumo ordenada por identificador
     */
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

    /**
     * Filtra instâncias ativas combinando múltiplos critérios opcionais.
     * Parâmetros nulos ou em branco são ignorados.
     *
     * @param identificacao  texto a buscar em identificador, patrimônio ou número de série
     * @param itemMestre     substring do nome do item mestre
     * @param categoriaId    UUID da categoria do item mestre
     * @param statusOperacional status operacional desejado
     * @return lista de DTOs ordenada por identificador, patrimônio e número de série
     */
    @Transactional(readOnly = true)
    public List<InstanciaItemResumoDTO> filtrar(String identificacao, String itemMestre, UUID categoriaId, StatusOperacionalInstancia statusOperacional) {
        return repository.findAll(
                        InstanciaItemRepository.filtrarAtivas(
                                ValidacoesBR.trimToNull(identificacao),
                                ValidacoesBR.trimToNull(itemMestre),
                                categoriaId,
                                statusOperacional
                        ),
                        Sort.by(Sort.Order.asc("identificador").nullsLast(), Sort.Order.asc("patrimonio").nullsLast(), Sort.Order.asc("numeroSerie").nullsLast())
                ).stream()
                .map(InstanciaItemMapper::toResumoDTO)
                .toList();
    }

    /**
     * Atualiza os dados de uma instância existente.
     *
     * @param id  UUID da instância a atualizar
     * @param dto dados de atualização validados pelo Bean Validation
     * @return DTO completo da instância atualizada
     * @throws jakarta.persistence.EntityNotFoundException se a instância não existir
     * @throws IllegalArgumentException se o item mestre ou local forem inválidos,
     *                                  se nenhum identificador for informado, ou se
     *                                  o status e o local forem incompatíveis
     */
    @Transactional
    public InstanciaItemResponseDTO atualizar(UUID id, InstanciaItemUpdateDTO dto) {
        InstanciaItem instancia = buscarPorId(id);
        UUID localAnteriorId = instancia.getLocalAtual() == null ? null : instancia.getLocalAtual().getId();
        ItemMestre itemMestre = buscarItemMestreAtivo(dto.itemMestreId());

        InstanciaItemMapper.updateEntity(instancia, dto);
        normalizarCampos(instancia);
        validarIdentificacao(instancia);
        instancia.setItemMestre(itemMestre);
        instancia.setLocalAtual(buscarLocalAtivo(dto.localAtualId()));
        InstanciaItemRegras.validarCoerenciaStatusLocal(instancia);

        InstanciaItem salva = salvar(instancia);
        UUID localAtualId = salva.getLocalAtual() == null ? null : salva.getLocalAtual().getId();
        String action = Objects.equals(localAnteriorId, localAtualId) ? "instance-updated" : "instance-location-updated";
        StructuredBusinessLogger.info(log, "inventory", action, StructuredBusinessLogger.fields(
                "instance_id", salva.getId(),
                "item_id", salva.getItemMestre() == null ? null : salva.getItemMestre().getId(),
                "previous_location_id", localAnteriorId,
                "location_id", localAtualId,
                "success", true
        ));
        return InstanciaItemMapper.toResponseDTO(salva);
    }

    @Transactional
    public void excluirLogicamente(UUID id) {
        if (movimentacaoItemRepository.existsByInstanciaItemId(id) || emprestimoItemRepository.existsByInstanciaItemId(id)) {
            throw new IllegalArgumentException("Instance with operational history cannot be deleted. Use the outbound operation to remove it from inventory.");
        }
        InstanciaItem instancia = buscarPorId(id);
        excluir(id);
        StructuredBusinessLogger.info(log, "inventory", "instance-deactivated", StructuredBusinessLogger.fields(
                "instance_id", id,
                "item_id", instancia.getItemMestre() == null ? null : instancia.getItemMestre().getId(),
                "location_id", instancia.getLocalAtual() == null ? null : instancia.getLocalAtual().getId(),
                "success", true
        ));
    }

    @Transactional(readOnly = true)
    public List<RevisaoDTO<InstanciaItem>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private ItemMestre buscarItemMestreAtivo(UUID itemMestreId) {
        ItemMestre itemMestre = itemMestreRepository.findById(itemMestreId)
                .orElseThrow(() -> new IllegalArgumentException("Main item not found."));
        if (!itemMestre.isAtivo()) {
            throw new IllegalArgumentException("Main item must be active.");
        }
        return itemMestre;
    }

    private LocalArmazenamento buscarLocalAtivo(UUID localId) {
        if (localId == null) {
            return null;
        }

        LocalArmazenamento local = localArmazenamentoRepository.findById(localId)
                .orElseThrow(() -> new IllegalArgumentException("Current location not found."));
        if (!local.isAtivo()) {
            throw new IllegalArgumentException("Current location must be active.");
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
            throw new IllegalArgumentException("Provide identifier, asset number or serial number for the instance.");
        }
    }
}
