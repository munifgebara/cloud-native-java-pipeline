package br.com.stella.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingConfigurationTest {

    private final YamlPropertySourceLoader yamlLoader = new YamlPropertySourceLoader();

    @Test
    void shouldConfigureConsoleReadableInProfileDefault() throws Exception {
        PropertySource<?> application = loadYaml("application.yml").getFirst();

        assertThat(application.getProperty("logging.pattern.console"))
                .isEqualTo("%d{HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n");
        assertThat(application.getProperty("logging.level.root"))
                .isEqualTo("${STELLA_LOG_LEVEL:INFO}");
    }

    @Test
    void shouldConfigureLogsStructuredInProfileServer() throws Exception {
        PropertySource<?> server = loadYaml("application-server.yml").getFirst();

        assertThat(server.getProperty("logging.structured.format.console")).isEqualTo("ecs");
        assertThat(server.getProperty("logging.structured.ecs.service.name")).isEqualTo("${spring.application.name}");
        assertThat(server.getProperty("logging.structured.ecs.service.environment")).isEqualTo("${STELLA_ENVIRONMENT:server}");
        assertThat(server.getProperty("logging.level.org.hibernate.orm.jdbc.bind")).isEqualTo("WARN");
    }

    @Test
    void shouldActivateProfileServerInDeploymentKubernetes() throws Exception {
        String configMap = Files.readString(Path.of("k8s/platform/stella-api/stella-api-configmap.yaml"));
        String deployment = Files.readString(Path.of("k8s/platform/stella-api/stella-api-deployment.yaml"));

        assertThat(configMap).contains("SPRING_PROFILES_ACTIVE: server");
        assertThat(configMap).contains("STELLA_LOG_LEVEL: INFO");
        assertThat(deployment).contains("observability.stella/log-format: ecs");
        assertThat(deployment).contains("observability.stella/log-collector: stdout");
    }

    private List<PropertySource<?>> loadYaml(String resource) throws Exception {
        return yamlLoader.load(resource, new ClassPathResource(resource));
    }
}
