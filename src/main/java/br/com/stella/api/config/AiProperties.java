package br.com.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(
        Boolean enabled
) {

    public AiProperties {
        if (enabled == null) {
            enabled = true;
        }
    }
}
