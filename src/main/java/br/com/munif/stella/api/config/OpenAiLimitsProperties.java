package br.com.munif.stella.api.config;

import br.com.munif.stella.api.service.AiOperation;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiLimitsProperties(
        Integer maxImagesPerDay,
        Integer maxGenerationsPerDay,
        Integer maxEmbeddingsPerDay
) {

    public Integer limitFor(AiOperation operation) {
        Integer limit = switch (operation) {
            case IMAGE_ANALYSIS -> maxImagesPerDay;
            case IMAGE_GENERATION -> maxGenerationsPerDay;
            case EMBEDDING -> maxEmbeddingsPerDay;
        };
        return limit == null || limit < 0 ? null : limit;
    }
}
