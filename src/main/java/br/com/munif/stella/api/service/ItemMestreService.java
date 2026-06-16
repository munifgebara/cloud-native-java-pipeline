package br.com.munif.stella.api.service;

import br.com.munif.comum.dto.RevisaoDTO;
import br.com.munif.comum.service.SuperService;
import br.com.munif.comum.utils.validacoes.ValidacoesBR;
import br.com.munif.stella.api.dto.ConsultaSemanticaItemDTO;
import br.com.munif.stella.api.dto.ItemMestreCreateDTO;
import br.com.munif.stella.api.dto.ImagemItemMestreDTO;
import br.com.munif.stella.api.dto.ItemMestreResponseDTO;
import br.com.munif.stella.api.dto.ItemMestreResumoDTO;
import br.com.munif.stella.api.dto.ItemMestreUpdateDTO;
import br.com.munif.stella.api.entity.Categoria;
import br.com.munif.stella.api.entity.ItemMestre;
import br.com.munif.stella.api.mapper.ItemMestreMapper;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import br.com.munif.stella.api.repository.CategoriaRepository;
import br.com.munif.stella.api.repository.ItemMestreRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável pelas operações de negócio sobre {@link ItemMestre}.
 *
 * <p>Gerencia o ciclo de vida dos itens mestres do inventário, incluindo persistência,
 * upload de imagem principal no MinIO e sincronização do índice de busca semântica
 * (pgvector) após cada alteração.</p>
 *
 * <p>A sincronização vetorial é executada <em>após o commit da transação</em> para garantir
 * que o índice somente reflita dados já confirmados no banco de dados relacional.</p>
 */
@Service
public class ItemMestreService extends SuperService<ItemMestre, ItemMestreRepository> {

    private static final Logger log = LoggerFactory.getLogger(ItemMestreService.class);

    private final CategoriaRepository categoriaRepository;
    private final ImagemItemMestreStorageService imagemStorageService;
    private final ItemMestreVectorSearchService vectorSearchService;

    public ItemMestreService(
            ItemMestreRepository repository,
            EntityManager entityManager,
            CategoriaRepository categoriaRepository,
            ImagemItemMestreStorageService imagemStorageService,
            ItemMestreVectorSearchService vectorSearchService
    ) {
        super(repository, entityManager, ItemMestre.class);
        this.categoriaRepository = categoriaRepository;
        this.imagemStorageService = imagemStorageService;
        this.vectorSearchService = vectorSearchService;
    }

    /**
     * Cria um novo item mestre e agenda a sincronização do índice vetorial.
     *
     * @param dto dados de criação validados pelo Bean Validation
     * @return DTO completo do item criado
     * @throws IllegalArgumentException se a categoria informada não existir ou estiver inativa
     */
    @Transactional
    public ItemMestreResponseDTO criar(ItemMestreCreateDTO dto) {
        ItemMestre item = ItemMestreMapper.toEntity(dto);
        normalizarCampos(item);
        item.setCategoria(buscarCategoriaAtiva(dto.categoriaId()));

        ItemMestre salvo = salvar(item);
        if (Boolean.FALSE.equals(dto.ativa())) {
            repository.flush();
            salvo.setAtivo(false);
            salvo = salvar(salvo);
        }
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-create");
        StructuredBusinessLogger.info(log, "inventory", "item-created", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getNome(),
                "category_id", salvo.getCategoria() == null ? null : salvo.getCategoria().getId(),
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Retorna o DTO completo de um item mestre pelo seu identificador.
     *
     * @param id UUID do item mestre
     * @return DTO completo do item
     * @throws jakarta.persistence.EntityNotFoundException se o item não existir
     */
    @Transactional(readOnly = true)
    public ItemMestreResponseDTO buscarResponsePorId(UUID id) {
        return ItemMestreMapper.toResponseDTO(buscarPorId(id));
    }

    /**
     * Lista todos os itens mestres ativos em ordem alfabética pelo nome.
     *
     * @return lista de DTOs de resumo dos itens ativos
     */
    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> listarResumo() {
        return repository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Lista todos os itens mestres, incluindo os inativados.
     *
     * @return lista de DTOs de resumo de todos os itens
     */
    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> listarResumoIncluindoInativos() {
        return listarTodosIncluindoInativos().stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Busca itens mestres ativos cujo nome contenha o texto informado (case-insensitive).
     *
     * @param nome substring a buscar; retorna lista vazia se em branco
     * @return lista de DTOs de resumo dos itens encontrados
     */
    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> buscarPorNome(String nome) {
        String nomeTratado = ValidacoesBR.trimToNull(nome);
        if (nomeTratado == null) {
            return List.of();
        }

        return repository.findByAtivoTrueAndNomeContainingIgnoreCaseOrderByNomeAsc(nomeTratado).stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Filtra itens mestres ativos combinando nome e categoria com {@code AND}.
     * Parâmetros nulos são ignorados.
     *
     * @param nome        substring do nome; ignorado se {@code null} ou em branco
     * @param categoriaId UUID da categoria; ignorado se {@code null}
     * @return lista de DTOs de resumo dos itens que atendem aos filtros
     */
    @Transactional(readOnly = true)
    public List<ItemMestreResumoDTO> filtrar(String nome, UUID categoriaId) {
        return repository.findAll(
                        ItemMestreRepository.filtrarAtivos(ValidacoesBR.trimToNull(nome), categoriaId),
                        Sort.by("nome").ascending()
                ).stream()
                .map(ItemMestreMapper::toResumoDTO)
                .toList();
    }

    /**
     * Atualiza os dados de um item mestre existente e reindexar o vetor de busca semântica.
     *
     * @param id  UUID do item a atualizar
     * @param dto dados de atualização validados pelo Bean Validation
     * @return DTO completo do item atualizado
     * @throws jakarta.persistence.EntityNotFoundException se o item não existir
     * @throws IllegalArgumentException se a categoria informada não existir ou estiver inativa
     */
    @Transactional
    public ItemMestreResponseDTO atualizar(UUID id, ItemMestreUpdateDTO dto) {
        ItemMestre item = buscarPorId(id);
        Categoria categoria = buscarCategoriaAtiva(dto.categoriaId());

        ItemMestreMapper.updateEntity(item, dto);
        normalizarCampos(item);
        item.setCategoria(categoria);

        ItemMestre salvo = salvar(item);
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-update");
        StructuredBusinessLogger.info(log, "inventory", "item-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getNome(),
                "category_id", salvo.getCategoria() == null ? null : salvo.getCategoria().getId(),
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Atualiza a imagem principal de um item mestre com um arquivo enviado pelo usuário.
     * Equivalente a chamar {@link #atualizarImagemPrincipal(UUID, MultipartFile, boolean, String)}
     * com {@code generatedByAi = false}.
     *
     * @param id      UUID do item mestre
     * @param arquivo arquivo de imagem enviado pelo cliente
     * @return DTO completo do item com os novos metadados de imagem
     */
    @Transactional
    public ItemMestreResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo) {
        return atualizarImagemPrincipal(id, arquivo, false, null);
    }

    /**
     * Atualiza a imagem principal de um item mestre, armazenando no MinIO e atualizando os metadados.
     * A imagem anterior é removida do bucket após o salvamento bem-sucedido da nova.
     * Ao final, o índice vetorial é ressincronizado.
     *
     * @param id             UUID do item mestre
     * @param arquivo        arquivo de imagem enviado
     * @param generatedByAi  {@code true} se a imagem foi gerada por IA
     * @param provider       nome do provedor de IA (ex.: "openai"); ignorado se {@code generatedByAi} for {@code false}
     * @return DTO completo do item com os novos metadados de imagem
     */
    @Transactional
    public ItemMestreResponseDTO atualizarImagemPrincipal(UUID id, MultipartFile arquivo, boolean generatedByAi, String provider) {
        ItemMestre item = buscarPorId(id);
        String bucketAnterior = item.getImagemBucket();
        String objectKeyAnterior = item.getImagemObjectKey();

        ImagemItemMestreDTO imagem = imagemStorageService.armazenar(id, arquivo);
        item.setImagemBucket(imagem.bucket());
        item.setImagemObjectKey(imagem.objectKey());
        item.setImagemContentType(imagem.contentType());
        item.setImagemTamanhoBytes(imagem.tamanhoBytes());
        item.setImagemGeneratedByAi(generatedByAi);
        item.setImagemProvider(generatedByAi ? ValidacoesBR.trimToNull(provider) : null);

        ItemMestre salvo = salvar(item);
        sincronizarIndiceVetorialSilenciosamente(salvo, "item-index-sync-after-image-update");
        imagemStorageService.removerSilenciosamente(bucketAnterior, objectKeyAnterior);
        StructuredBusinessLogger.info(log, "inventory", "item-image-updated", StructuredBusinessLogger.fields(
                "item_id", salvo.getId(),
                "item_name", salvo.getNome(),
                "image_content_type", imagem.contentType(),
                "image_size_bytes", imagem.tamanhoBytes(),
                "image_generated_by_ai", generatedByAi,
                "ai_provider", generatedByAi ? ValidacoesBR.trimToNull(provider) : null,
                "success", true
        ));
        return ItemMestreMapper.toResponseDTO(salvo);
    }

    /**
     * Retorna os metadados (bucket, objectKey, contentType e tamanho) da imagem principal de um item.
     *
     * @param id UUID do item mestre
     * @return DTO com os metadados necessários para recuperar o arquivo no MinIO
     * @throws IllegalArgumentException se o item não possuir imagem cadastrada
     */
    @Transactional(readOnly = true)
    public ImagemItemMestreDTO buscarMetadadosImagemPrincipal(UUID id) {
        ItemMestre item = buscarPorId(id);
        if (item.getImagemObjectKey() == null) {
            throw new IllegalArgumentException("Main item does not have a main image.");
        }
        return new ImagemItemMestreDTO(
                item.getImagemBucket(),
                item.getImagemObjectKey(),
                item.getImagemContentType(),
                item.getImagemTamanhoBytes()
        );
    }

    /**
     * Abre um stream de leitura da imagem principal do item no MinIO.
     * O chamador é responsável por fechar o stream após o uso.
     *
     * @param id UUID do item mestre
     * @return stream de leitura da imagem
     * @throws IllegalArgumentException se o item não possuir imagem cadastrada
     */
    @Transactional(readOnly = true)
    public InputStream abrirImagemPrincipal(UUID id) {
        ImagemItemMestreDTO imagem = buscarMetadadosImagemPrincipal(id);
        return imagemStorageService.abrir(imagem.bucket(), imagem.objectKey());
    }

    /**
     * Inativa logicamente um item mestre e remove sua entrada do índice vetorial.
     *
     * @param id UUID do item a inativar
     * @throws jakarta.persistence.EntityNotFoundException se o item não existir
     */
    @Transactional
    public void excluirLogicamente(UUID id) {
        ItemMestre item = buscarPorId(id);
        excluir(id);
        removerIndiceVetorialSilenciosamente(id, item.getNome());
        StructuredBusinessLogger.info(log, "inventory", "item-deactivated", StructuredBusinessLogger.fields(
                "item_id", id,
                "item_name", item.getNome(),
                "success", true
        ));
    }

    /**
     * Realiza busca semântica no índice vetorial de itens mestres.
     *
     * @param consulta texto livre descrevendo o que se procura
     * @return lista de resultados ordenados por similaridade semântica
     */
    @Transactional(readOnly = true)
    public List<ConsultaSemanticaItemDTO> buscarSemanticamente(String consulta) {
        return vectorSearchService.buscar(consulta);
    }

    /**
     * Força a reindexação vetorial de todos os itens mestres ativos.
     * Gera embeddings para cada item e atualiza o índice pgvector.
     *
     * @return número de itens reindexados
     */
    @Transactional
    public int reindexarBuscaSemantica() {
        return vectorSearchService.reindexarItensAtivos();
    }

    /**
     * Retorna o histórico de revisões anteriores de um item mestre (Hibernate Envers).
     *
     * @param id UUID do item mestre
     * @return lista de revisões em ordem cronológica; lista vazia se não houver histórico
     */
    @Transactional(readOnly = true)
    public List<RevisaoDTO<ItemMestre>> listarRevisoes(UUID id) {
        return listarVersoesAnteriores(id);
    }

    private Categoria buscarCategoriaAtiva(UUID categoriaId) {
        if (categoriaId == null) {
            return null;
        }

        Categoria categoria = categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found."));
        if (!categoria.isAtivo()) {
            throw new IllegalArgumentException("Category must be active.");
        }
        return categoria;
    }

    private void normalizarCampos(ItemMestre item) {
        item.setNome(ValidacoesBR.trimToNull(item.getNome()));
        item.setDescricao(ValidacoesBR.trimToNull(item.getDescricao()));
        item.setObservacoes(ValidacoesBR.trimToNull(item.getObservacoes()));
    }

    private void sincronizarIndiceVetorialSilenciosamente(ItemMestre item, String action) {
        executarAposCommit(action, item == null ? null : item.getId(), item == null ? null : item.getNome(),
                () -> vectorSearchService.sincronizar(item));
    }

    private void removerIndiceVetorialSilenciosamente(UUID id, String nome) {
        executarAposCommit("item-index-remove-after-delete", id, nome, () -> vectorSearchService.remover(id));
    }

    private void executarAposCommit(String action, UUID itemId, String itemName, Runnable operation) {
        Runnable guardedOperation = () -> {
            try {
                operation.run();
            } catch (RuntimeException ex) {
                StructuredBusinessLogger.warn(log, "vector-search", action, StructuredBusinessLogger.fields(
                        "item_id", itemId,
                        "item_name", itemName,
                        "success", false,
                        "failure_type", ex.getClass().getSimpleName()
                ));
            }
        };

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            guardedOperation.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                guardedOperation.run();
            }
        });
    }
}
