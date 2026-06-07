package br.com.munif.stella.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stella.keycloak")
public record KeycloakProperties(
        String baseUrl,
        String realm,
        String publicClientId,
        String adminRealm,
        String adminClientId,
        String adminUsername,
        String adminPassword
) {
    public KeycloakProperties {
        adminRealm = adminRealm == null || adminRealm.isBlank() ? "master" : adminRealm;
        adminClientId = adminClientId == null || adminClientId.isBlank() ? "admin-cli" : adminClientId;
    }

    public String tokenUrl() {
        return baseUrl + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String adminTokenUrl() {
        return baseUrl + "/realms/" + adminRealm + "/protocol/openid-connect/token";
    }

    public String adminRealmUrl() {
        return baseUrl + "/admin/realms/" + realm;
    }

    public String accountUrl() {
        return baseUrl + "/realms/" + realm + "/account";
    }
}
