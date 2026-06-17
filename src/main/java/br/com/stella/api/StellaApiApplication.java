package br.com.stella.api;

import br.com.stella.api.config.AiProperties;
import br.com.stella.api.config.CorsProperties;
import br.com.stella.api.config.EmbeddingsProperties;
import br.com.stella.api.config.KeycloakProperties;
import br.com.stella.api.config.OpenAiLimitsProperties;
import br.com.stella.api.config.VectorSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
		"br.com.stella.api",
		"br.com.munif.common.persistencia"
})
@EnableConfigurationProperties({KeycloakProperties.class, CorsProperties.class, EmbeddingsProperties.class, VectorSearchProperties.class, AiProperties.class, OpenAiLimitsProperties.class})
public class StellaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StellaApiApplication.class, args);
	}

}
