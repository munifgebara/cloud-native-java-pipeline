package br.com.munif.pagadoria.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;

@SpringBootApplication
@EntityScan(basePackages = {
		"br.com.munif.pagadoria.api",
		"br.com.munif.comum.persistencia"
})
public class PagadoriaApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PagadoriaApiApplication.class, args);
	}

}
