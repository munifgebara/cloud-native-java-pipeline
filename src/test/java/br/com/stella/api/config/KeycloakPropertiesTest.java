package br.com.stella.api.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakPropertiesTest {

    @Test
    void shouldDeriveIssuerAndJwkSetUrlWhenNotConfiguredExplicitly() {
        var properties = new KeycloakProperties(
                "https://keycloak.example.test",
                "stella",
                "stella-cli",
                null,
                null,
                "admin",
                "admin",
                null,
                null,
                null
        );

        assertThat(properties.issuerUrl()).isEqualTo("https://keycloak.example.test/realms/stella");
        assertThat(properties.jwkSetUrl()).isEqualTo("https://keycloak.example.test/realms/stella/protocol/openid-connect/certs");
    }

    @Test
    void shouldUseExplicitIssuerAndJwkSetUrlWhenConfigured() {
        var properties = new KeycloakProperties(
                "http://keycloak.platform.svc.cluster.local:8080",
                "stella",
                "stella-cli",
                null,
                null,
                "admin",
                "admin",
                null,
                "https://keycloak.gebaralabs.dev/realms/stella",
                "http://keycloak.platform.svc.cluster.local:8080/realms/stella/protocol/openid-connect/certs"
        );

        assertThat(properties.issuerUrl()).isEqualTo("https://keycloak.gebaralabs.dev/realms/stella");
        assertThat(properties.jwkSetUrl()).isEqualTo("http://keycloak.platform.svc.cluster.local:8080/realms/stella/protocol/openid-connect/certs");
    }
}
