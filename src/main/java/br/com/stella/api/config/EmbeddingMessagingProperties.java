package br.com.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stella.messaging")
public record EmbeddingMessagingProperties(
        boolean enabled,
        String exchange,
        String queue,
        String routingKey,
        String deadLetterExchange,
        String deadLetterQueue,
        String deadLetterRoutingKey,
        int relayBatchSize,
        int publishMaxAttempts,
        int confirmTimeoutSeconds
) {

    public EmbeddingMessagingProperties {
        exchange = defaultIfBlank(exchange, "stella.embedding");
        queue = defaultIfBlank(queue, "stella.embedding.index");
        routingKey = defaultIfBlank(routingKey, "embedding.index");
        deadLetterExchange = defaultIfBlank(deadLetterExchange, "stella.embedding.dlx");
        deadLetterQueue = defaultIfBlank(deadLetterQueue, "stella.embedding.index.dlq");
        deadLetterRoutingKey = defaultIfBlank(deadLetterRoutingKey, "embedding.index.failed");
        relayBatchSize = relayBatchSize > 0 ? relayBatchSize : 50;
        publishMaxAttempts = publishMaxAttempts > 0 ? publishMaxAttempts : 10;
        confirmTimeoutSeconds = confirmTimeoutSeconds > 0 ? confirmTimeoutSeconds : 5;
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
