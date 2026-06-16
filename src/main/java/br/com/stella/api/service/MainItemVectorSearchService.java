package br.com.stella.api.service;

import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.config.EmbeddingsProperties;
import br.com.stella.api.config.VectorSearchProperties;
import br.com.stella.api.dto.ConsultaSemanticaInstanciaDTO;
import br.com.stella.api.dto.ConsultaSemanticaItemDTO;
import br.com.stella.api.dto.ConsultaSemanticaLocalDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MainItemVectorSearchService {

    private static final Logger log = LoggerFactory.getLogger(MainItemVectorSearchService.class);

    private static final String UPSERT_SQL = """
            insert into public.item_mestre_embedding
                (item_mestre_id, provider, modelo, dimensoes, texto_indexado, embedding, ativo, alterado_em)
            values (?, ?, ?, ?, ?, ?::vector, true, now())
            on conflict (item_mestre_id) do update set
                provider = excluded.provider,
                modelo = excluded.modelo,
                dimensoes = excluded.dimensoes,
                texto_indexado = excluded.texto_indexado,
                embedding = excluded.embedding,
                ativo = true,
                alterado_em = now()
            """;

    private static final String REMOVER_SQL = """
            update public.item_mestre_embedding
            set ativo = false, alterado_em = now()
            where item_mestre_id = ?
            """;

    private static final String BUSCAR_SQL = """
            select e.item_mestre_id, 1 - (e.embedding <=> ?::vector) as similaridade
            from public.item_mestre_embedding e
            join public.item_mestre i on i.id = e.item_mestre_id
            where e.ativo = true
              and i.ativo = true
            order by e.embedding <=> ?::vector
            limit ?
            """;

    private final VectorSearchProperties vectorSearchProperties;
    private final EmbeddingsProperties embeddingsProperties;
    private final EmbeddingProvider embeddingProvider;
    private final ItemMestreEmbeddingDocumentFactory documentFactory;
    private final JdbcTemplate jdbcTemplate;
    private final MainItemRepository itemMestreRepository;
    private final ItemInstanceRepository instanciaItemRepository;
    private final ConsultaVetorialMetricasService consultaVetorialMetricasService;
    private final AiUsageGuard aiUsageGuard;

    public MainItemVectorSearchService(
            VectorSearchProperties vectorSearchProperties,
            EmbeddingsProperties embeddingsProperties,
            EmbeddingProvider embeddingProvider,
            ItemMestreEmbeddingDocumentFactory documentFactory,
            JdbcTemplate jdbcTemplate,
            MainItemRepository itemMestreRepository,
            ItemInstanceRepository instanciaItemRepository,
            ConsultaVetorialMetricasService consultaVetorialMetricasService,
            AiUsageGuard aiUsageGuard
    ) {
        this.vectorSearchProperties = vectorSearchProperties;
        this.embeddingsProperties = embeddingsProperties;
        this.embeddingProvider = embeddingProvider;
        this.documentFactory = documentFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.itemMestreRepository = itemMestreRepository;
        this.instanciaItemRepository = instanciaItemRepository;
        this.consultaVetorialMetricasService = consultaVetorialMetricasService;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sincronizar(MainItem item) {
        if (!vectorSearchProperties.enabled() || item == null || item.getId() == null) {
            return;
        }
        long inicio = System.nanoTime();

        if (!item.isActive()) {
            remover(item.getId());
            return;
        }

        String documento = documentFactory.criarDocumento(item);
        if (documento.isBlank()) {
            remover(item.getId());
            return;
        }

        try {
            aiUsageGuard.consume(AiOperation.EMBEDDING);
            float[] embedding = embeddingProvider.gerarEmbedding(documento);
            validarDimensoes(embedding);

            jdbcTemplate.update(
                    UPSERT_SQL,
                    item.getId(),
                    embeddingsProperties.provider(),
                    embeddingsProperties.model(),
                    embeddingsProperties.dimensions(),
                    documento,
                    vectorLiteral(embedding)
            );
            StructuredBusinessLogger.info(log, "vector-search", "item-indexed", StructuredBusinessLogger.fields(
                    "item_id", item.getId(),
                    "item_name", item.getName(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "item-indexed", StructuredBusinessLogger.fields(
                    "item_id", item.getId(),
                    "item_name", item.getName(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw ex;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remover(UUID itemMestreId) {
        if (!vectorSearchProperties.enabled() || itemMestreId == null) {
            return;
        }
        long inicio = System.nanoTime();
        try {
            jdbcTemplate.update(REMOVER_SQL, itemMestreId);
            StructuredBusinessLogger.info(log, "vector-search", "item-index-removed", StructuredBusinessLogger.fields(
                    "item_id", itemMestreId,
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "item-index-removed", StructuredBusinessLogger.fields(
                    "item_id", itemMestreId,
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw ex;
        }
    }

    @Transactional
    public int reindexarItensAtivos() {
        if (!vectorSearchProperties.enabled()) {
            return 0;
        }

        long inicio = System.nanoTime();
        List<MainItem> itens = itemMestreRepository.findByActiveTrueOrderByNameAsc();
        try {
            itens.forEach(this::sincronizar);
            StructuredBusinessLogger.info(log, "vector-search", "items-reindexed", StructuredBusinessLogger.fields(
                    "items_count", itens.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));
            return itens.size();
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "items-reindexed", StructuredBusinessLogger.fields(
                    "items_count", itens.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<ConsultaSemanticaItemDTO> buscar(String consulta) {
        String texto = BrValidations.trimToNull(consulta);
        if (!vectorSearchProperties.enabled() || texto == null) {
            return List.of();
        }

        long inicio = System.nanoTime();
        try {
            aiUsageGuard.consume(AiOperation.EMBEDDING);
            float[] embedding = embeddingProvider.gerarEmbedding(texto);
            validarDimensoes(embedding);
            String literal = vectorLiteral(embedding);

            List<ResultadoVetorial> resultados = jdbcTemplate.query(
                    BUSCAR_SQL,
                    (rs, rowNum) -> new ResultadoVetorial(
                            rs.getObject("item_mestre_id", UUID.class),
                            rs.getDouble("similaridade")
                    ),
                    literal,
                    literal,
                    vectorSearchProperties.maxResults()
            ).stream()
                    .filter(resultado -> resultado.similaridade() >= vectorSearchProperties.minSimilarity())
                    .toList();

            consultaVetorialMetricasService.registrarConsulta(texto, resultados.size());
            StructuredBusinessLogger.info(log, "vector-search", "semantic-query", StructuredBusinessLogger.fields(
                    "query_text", truncate(texto, 200),
                    "results_count", resultados.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));

            if (resultados.isEmpty()) {
                return List.of();
            }

            return montarResposta(resultados);
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "semantic-query", StructuredBusinessLogger.fields(
                    "query_text", truncate(texto, 200),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw ex;
        }
    }

    private List<ConsultaSemanticaItemDTO> montarResposta(List<ResultadoVetorial> resultados) {
        List<UUID> ids = resultados.stream().map(ResultadoVetorial::itemMestreId).toList();
        Map<UUID, Double> similaridades = resultados.stream()
                .collect(Collectors.toMap(ResultadoVetorial::itemMestreId, ResultadoVetorial::similaridade));

        Map<UUID, MainItem> itensPorId = itemMestreRepository.buscarComCategoriaPorIds(ids).stream()
                .collect(Collectors.toMap(MainItem::getId, item -> item));
        Map<UUID, List<ItemInstance>> instanciasPorItem = instanciaItemRepository.buscarAtivasPorItemMestreIds(ids).stream()
                .collect(Collectors.groupingBy(instance -> instance.getMainItem().getId()));

        List<ConsultaSemanticaItemDTO> resposta = new ArrayList<>();
        for (UUID id : ids) {
            MainItem item = itensPorId.get(id);
            if (item == null || !item.isActive()) {
                continue;
            }

            List<ItemInstance> instancias = instanciasPorItem.getOrDefault(id, List.of());
            resposta.add(new ConsultaSemanticaItemDTO(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getCategory() == null ? null : item.getCategory().getName(),
                    item.getCategory() == null ? null : item.getCategory().getIcon(),
                    imagemUrl(item),
                    arredondar(similaridades.getOrDefault(id, 0.0)),
                    instancias.stream().map(this::toInstanciaDTO).toList(),
                    locaisProvaveis(instancias)
            ));
        }

        return resposta;
    }

    private ConsultaSemanticaInstanciaDTO toInstanciaDTO(ItemInstance instance) {
        StorageLocation location = instance.getCurrentLocation();
        return new ConsultaSemanticaInstanciaDTO(
                instance.getId(),
                instance.getIdentifier(),
                instance.getAssetTag(),
                instance.getSerialNumber(),
                instance.getOperationalStatus(),
                location == null ? null : location.getId(),
                location == null ? null : location.getName()
        );
    }

    private List<ConsultaSemanticaLocalDTO> locaisProvaveis(List<ItemInstance> instancias) {
        return instancias.stream()
                .map(ItemInstance::getCurrentLocation)
                .filter(location -> location != null && location.isActive())
                .collect(Collectors.groupingBy(StorageLocation::getId, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(locais -> new ConsultaSemanticaLocalDTO(
                        locais.getFirst().getId(),
                        locais.getFirst().getName(),
                        locais.size()
                ))
                .sorted(Comparator.comparing(ConsultaSemanticaLocalDTO::quantidade).reversed()
                        .thenComparing(ConsultaSemanticaLocalDTO::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void validarDimensoes(float[] embedding) {
        if (embedding == null || embedding.length != embeddingsProperties.dimensions()) {
            throw new ExternalIntegrationException("Embeddings provider returned a vector with incompatible dimensions.");
        }
    }

    private String vectorLiteral(float[] embedding) {
        StringBuilder literal = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                literal.append(',');
            }
            literal.append(String.format(Locale.US, "%.8f", embedding[i]));
        }
        return literal.append(']').toString();
    }

    private String imagemUrl(MainItem item) {
        return item.getImagemObjectKey() == null ? null : "/api/public/itens-mestre/%s/imagem-principal".formatted(item.getId());
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10000.0) / 10000.0;
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record ResultadoVetorial(UUID itemMestreId, double similaridade) {
    }
}
