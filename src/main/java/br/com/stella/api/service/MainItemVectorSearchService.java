package br.com.stella.api.service;

import br.com.munif.common.utils.validacoes.BrValidations;
import br.com.stella.api.config.EmbeddingsProperties;
import br.com.stella.api.config.VectorSearchProperties;
import br.com.stella.api.dto.SemanticSearchInstanceDTO;
import br.com.stella.api.dto.SemanticSearchItemDTO;
import br.com.stella.api.dto.SemanticSearchLocationDTO;
import br.com.stella.api.entity.ItemInstance;
import br.com.stella.api.entity.MainItem;
import br.com.stella.api.entity.StorageLocation;
import br.com.stella.api.exception.AiUsageLimitException;
import br.com.stella.api.exception.ExternalIntegrationException;
import br.com.stella.api.observability.StructuredBusinessLogger;
import br.com.stella.api.repository.ItemInstanceRepository;
import br.com.stella.api.repository.MainItemRepository;
import org.springframework.dao.DataAccessException;
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
            insert into public.main_item_embedding
                (main_item_id, provider, model, dimensions, indexed_text, embedding, active, updated_at)
            values (?, ?, ?, ?, ?, ?::vector, true, now())
            on conflict (main_item_id) do update set
                provider = excluded.provider,
                model = excluded.model,
                dimensions = excluded.dimensions,
                indexed_text = excluded.indexed_text,
                embedding = excluded.embedding,
                active = true,
                updated_at = now()
            """;

    private static final String REMOVER_SQL = """
            update public.main_item_embedding
            set active = false, updated_at = now()
            where main_item_id = ?
            """;

    private static final String BUSCAR_SQL = """
            select e.main_item_id, 1 - (e.embedding <=> ?::vector) as similarity
            from public.main_item_embedding e
            join public.main_item i on i.id = e.main_item_id
            where e.active = true
              and i.active = true
            order by e.embedding <=> ?::vector
            limit ?
            """;

    private final VectorSearchProperties vectorSearchProperties;
    private final EmbeddingsProperties embeddingsProperties;
    private final EmbeddingProvider embeddingProvider;
    private final MainItemEmbeddingDocumentFactory documentFactory;
    private final JdbcTemplate jdbcTemplate;
    private final MainItemRepository mainItemRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final VectorSearchMetricsService vectorSearchMetricsService;
    private final AiUsageGuard aiUsageGuard;

    public MainItemVectorSearchService(
            VectorSearchProperties vectorSearchProperties,
            EmbeddingsProperties embeddingsProperties,
            EmbeddingProvider embeddingProvider,
            MainItemEmbeddingDocumentFactory documentFactory,
            JdbcTemplate jdbcTemplate,
            MainItemRepository mainItemRepository,
            ItemInstanceRepository itemInstanceRepository,
            VectorSearchMetricsService vectorSearchMetricsService,
            AiUsageGuard aiUsageGuard
    ) {
        this.vectorSearchProperties = vectorSearchProperties;
        this.embeddingsProperties = embeddingsProperties;
        this.embeddingProvider = embeddingProvider;
        this.documentFactory = documentFactory;
        this.jdbcTemplate = jdbcTemplate;
        this.mainItemRepository = mainItemRepository;
        this.itemInstanceRepository = itemInstanceRepository;
        this.vectorSearchMetricsService = vectorSearchMetricsService;
        this.aiUsageGuard = aiUsageGuard;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void synchronize(MainItem item) {
        if (!vectorSearchProperties.enabled() || item == null || item.getId() == null) {
            return;
        }
        long inicio = System.nanoTime();

        if (!item.isActive()) {
            remove(item.getId());
            return;
        }

        String document = documentFactory.createDocument(item);
        if (document.isBlank()) {
            remove(item.getId());
            return;
        }

        try {
            aiUsageGuard.consume(AiOperation.EMBEDDING);
            float[] embedding = embeddingProvider.generateEmbedding(document);
            validateDimensions(embedding);

            jdbcTemplate.update(
                    UPSERT_SQL,
                    item.getId(),
                    embeddingsProperties.provider(),
                    embeddingsProperties.model(),
                    embeddingsProperties.dimensions(),
                    document,
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
            throw externalIntegrationException("Unable to update the vector search index.", ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void remove(UUID mainItemId) {
        if (!vectorSearchProperties.enabled() || mainItemId == null) {
            return;
        }
        long inicio = System.nanoTime();
        try {
            jdbcTemplate.update(REMOVER_SQL, mainItemId);
            StructuredBusinessLogger.info(log, "vector-search", "item-index-removed", StructuredBusinessLogger.fields(
                    "item_id", mainItemId,
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "item-index-removed", StructuredBusinessLogger.fields(
                    "item_id", mainItemId,
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw externalIntegrationException("Unable to update the vector search index.", ex);
        }
    }

    @Transactional
    public int reindexActiveItems() {
        if (!vectorSearchProperties.enabled()) {
            return 0;
        }

        long inicio = System.nanoTime();
        List<MainItem> items = mainItemRepository.findByActiveTrueOrderByNameAsc();
        try {
            items.forEach(this::synchronize);
            StructuredBusinessLogger.info(log, "vector-search", "items-reindexed", StructuredBusinessLogger.fields(
                    "items_count", items.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));
            return items.size();
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "items-reindexed", StructuredBusinessLogger.fields(
                    "items_count", items.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw externalIntegrationException("Unable to reindex vector search items.", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<SemanticSearchItemDTO> search(String query) {
        String texto = BrValidations.trimToNull(query);
        if (!vectorSearchProperties.enabled() || texto == null) {
            return List.of();
        }

        long inicio = System.nanoTime();
        try {
            aiUsageGuard.consume(AiOperation.EMBEDDING);
            float[] embedding = embeddingProvider.generateEmbedding(texto);
            validateDimensions(embedding);
            String literal = vectorLiteral(embedding);

            List<VectorResult> results = jdbcTemplate.query(
                    BUSCAR_SQL,
                    (rs, rowNum) -> new VectorResult(
                            rs.getObject("main_item_id", UUID.class),
                            rs.getDouble("similarity")
                    ),
                    literal,
                    literal,
                    vectorSearchProperties.maxResults()
            ).stream()
                    .filter(result -> result.similarity() >= vectorSearchProperties.minSimilarity())
                    .toList();

            vectorSearchMetricsService.recordQuery(texto, results.size());
            StructuredBusinessLogger.info(log, "vector-search", "semantic-query", StructuredBusinessLogger.fields(
                    "query_text", truncate(texto, 200),
                    "results_count", results.size(),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", true
            ));

            if (results.isEmpty()) {
                return List.of();
            }

            return buildResponse(results);
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "semantic-query", StructuredBusinessLogger.fields(
                    "query_text", truncate(texto, 200),
                    "embeddings_provider", embeddingsProperties.provider(),
                    "embeddings_model", embeddingsProperties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw externalIntegrationException("Unable to execute semantic search.", ex);
        }
    }

    private RuntimeException externalIntegrationException(String message, RuntimeException ex) {
        if (ex instanceof ExternalIntegrationException || ex instanceof AiUsageLimitException) {
            return ex;
        }
        if (ex instanceof DataAccessException || ex.getCause() instanceof DataAccessException) {
            return new ExternalIntegrationException(message, ex);
        }
        return ex;
    }

    private List<SemanticSearchItemDTO> buildResponse(List<VectorResult> results) {
        List<UUID> ids = results.stream().map(VectorResult::mainItemId).toList();
        Map<UUID, Double> similaridades = results.stream()
                .collect(Collectors.toMap(VectorResult::mainItemId, VectorResult::similarity));

        Map<UUID, MainItem> itensPorId = mainItemRepository.findWithCategoryByIds(ids).stream()
                .collect(Collectors.toMap(MainItem::getId, item -> item));
        Map<UUID, List<ItemInstance>> instancesByItem = itemInstanceRepository.findActiveByMainItemIds(ids).stream()
                .collect(Collectors.groupingBy(instance -> instance.getMainItem().getId()));

        List<SemanticSearchItemDTO> response = new ArrayList<>();
        for (UUID id : ids) {
            MainItem item = itensPorId.get(id);
            if (item == null || !item.isActive()) {
                continue;
            }

            List<ItemInstance> instances = instancesByItem.getOrDefault(id, List.of());
            response.add(new SemanticSearchItemDTO(
                    item.getId(),
                    item.getName(),
                    item.getDescription(),
                    item.getCategory() == null ? null : item.getCategory().getName(),
                    item.getCategory() == null ? null : item.getCategory().getIcon(),
                    imageUrl(item),
                    arredondar(similaridades.getOrDefault(id, 0.0)),
                    instances.stream().map(this::toInstanceDTO).toList(),
                    locaisProvaveis(instances)
            ));
        }

        return response;
    }

    private SemanticSearchInstanceDTO toInstanceDTO(ItemInstance instance) {
        StorageLocation location = instance.getCurrentLocation();
        return new SemanticSearchInstanceDTO(
                instance.getId(),
                instance.getIdentifier(),
                instance.getAssetTag(),
                instance.getSerialNumber(),
                instance.getOperationalStatus(),
                location == null ? null : location.getId(),
                location == null ? null : location.getName()
        );
    }

    private List<SemanticSearchLocationDTO> locaisProvaveis(List<ItemInstance> instances) {
        return instances.stream()
                .map(ItemInstance::getCurrentLocation)
                .filter(location -> location != null && location.isActive())
                .collect(Collectors.groupingBy(StorageLocation::getId, LinkedHashMap::new, Collectors.toList()))
                .values()
                .stream()
                .map(locations -> new SemanticSearchLocationDTO(
                        locations.getFirst().getId(),
                        locations.getFirst().getName(),
                        locations.size()
                ))
                .sorted(Comparator.comparing(SemanticSearchLocationDTO::quantity).reversed()
                        .thenComparing(SemanticSearchLocationDTO::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void validateDimensions(float[] embedding) {
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

    private String imageUrl(MainItem item) {
        return item.getImageObjectKey() == null ? null : "/api/public/main-items/%s/main-image".formatted(item.getId());
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

    private record VectorResult(UUID mainItemId, double similarity) {
    }
}
