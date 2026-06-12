package br.com.munif.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stella.embeddings")
public record EmbeddingsProperties(
        String provider,
        String baseUrl,
        String model,
        int dimensions
) {

    public EmbeddingsProperties {
        if (provider == null || provider.isBlank()) {
            provider = "local";
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://stella-embeddings:8000";
        }
        if (model == null || model.isBlank()) {
            model = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2";
        }
        if (dimensions <= 0) {
            dimensions = 384;
        }
    }
}
