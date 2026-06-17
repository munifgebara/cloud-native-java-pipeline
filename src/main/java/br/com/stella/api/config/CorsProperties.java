package br.com.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "stella.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}