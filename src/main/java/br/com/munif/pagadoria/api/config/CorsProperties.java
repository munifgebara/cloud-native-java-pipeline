package br.com.munif.pagadoria.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "pagadoria.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}