package br.com.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stella.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        long maxImageSizeBytes
) {
    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://127.0.0.1:9000";
        }
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = "minioadmin";
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "minioadmin";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "stella-items";
        }
        if (maxImageSizeBytes <= 0) {
            maxImageSizeBytes = 5 * 1024 * 1024;
        }
    }
}
