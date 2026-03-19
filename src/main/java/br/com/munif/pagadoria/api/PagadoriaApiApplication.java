package br.com.munif.pagadoria.api;

import br.com.munif.pagadoria.api.config.CorsProperties;
import br.com.munif.pagadoria.api.config.KeycloakProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
		"br.com.munif.pagadoria.api",
		"br.com.munif.comum.persistencia"
})
@EnableConfigurationProperties({KeycloakProperties.class, CorsProperties.class})
public class PagadoriaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagadoriaApiApplication.class, args);
	}

}
