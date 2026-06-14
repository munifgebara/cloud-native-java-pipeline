package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.EmbeddingsProperties;
import br.com.munif.stella.api.observability.StructuredBusinessLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import br.com.munif.stella.api.exception.IntegracaoExternaException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LocalEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalEmbeddingProvider.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingsProperties properties;

    @Autowired
    public LocalEmbeddingProvider(EmbeddingsProperties properties) {
        this.properties = properties;
        this.embeddingModel = new OpenAiEmbeddingModel(
                OpenAiEmbeddingOptions.builder()
                        .baseUrl(properties.baseUrl())
                        .model(properties.model())
                        .apiKey("local")
                        .build()
        );
    }

    LocalEmbeddingProvider(EmbeddingModel embeddingModel, EmbeddingsProperties properties) {
        this.embeddingModel = embeddingModel;
        this.properties = properties;
    }

    @Override
    public float[] gerarEmbedding(String texto) {
        long inicio = System.nanoTime();
        try {
            float[] embedding = embeddingModel.embed(texto);
            StructuredBusinessLogger.info(log, "vector-search", "embedding-generated", StructuredBusinessLogger.fields(
                    "embeddings_provider", properties.provider(),
                    "embeddings_model", properties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "embedding_dimensions", embedding.length,
                    "success", true
            ));
            return embedding;
        } catch (RuntimeException ex) {
            StructuredBusinessLogger.error(log, "vector-search", "embedding-generated", StructuredBusinessLogger.fields(
                    "embeddings_provider", properties.provider(),
                    "embeddings_model", properties.model(),
                    "duration_ms", elapsedMillis(inicio),
                    "success", false
            ), ex);
            throw new IntegracaoExternaException("Falha ao consultar o provider local de embeddings.", ex);
        }
    }

    private long elapsedMillis(long inicio) {
        return (System.nanoTime() - inicio) / 1_000_000L;
    }
}
