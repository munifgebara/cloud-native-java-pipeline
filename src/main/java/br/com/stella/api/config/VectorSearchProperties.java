package br.com.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stella.vector-search")
public record VectorSearchProperties(
        boolean enabled,
        double minSimilarity,
        int maxResults
) {

    public VectorSearchProperties {
        if (minSimilarity <= 0) {
            minSimilarity = 0.20;
        }
        if (maxResults <= 0) {
            maxResults = 12;
        }
    }
}
