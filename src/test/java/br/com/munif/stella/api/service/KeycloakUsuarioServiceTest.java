package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.KeycloakProperties;
import br.com.munif.stella.api.dto.AlterarSenhaDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.exception.IdentidadeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KeycloakUsuarioServiceTest {

    private MockRestServiceServer server;
    private KeycloakUsuarioService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakUsuarioService(
                new KeycloakProperties("http://keycloak", "stella", "stella-cli", "master", "admin-cli", "admin", "admin", null),
                builder
        );
    }

    @Test
    void deveListarUsuariosComRolesDoKeycloak() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users?briefRepresentation=false"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withSuccess("""
                        [
                          {
                            "id": "user-1",
                            "username": "admin",
                            "firstName": "Admin",
                            "lastName": "Stella",
                            "email": "admin@example.local",
                            "enabled": true
                          }
                        ]
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-1/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withSuccess("""
                        [
                          {"id": "role-1", "name": "admin"},
                          {"id": "role-default", "name": "default-roles-stella"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        List<UsuarioResponseDTO> usuarios = service.listar();

        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.getFirst().username()).isEqualTo("admin");
        assertThat(usuarios.getFirst().roles()).containsExactly("admin");
        server.verify();
    }

    @Test
    void deveCriarUsuarioComSenhaERoles() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withCreatedEntity(URI.create("http://keycloak/admin/realms/stella/users/user-2")));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-2/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/roles/usuario"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id": "role-usuario", "name": "usuario"}
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-2/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        expectUsuarioCompleto("user-2", "novo");

        UsuarioResponseDTO usuario = service.criar(new UsuarioCreateDTO(
                "novo",
                "Novo",
                "Usuario",
                "novo@example.local",
                "segredo123",
                true,
                List.of("usuario")
        ));

        assertThat(usuario.id()).isEqualTo("user-2");
        assertThat(usuario.roles()).containsExactly("usuario");
        server.verify();
    }

    @Test
    void deveValidarSenhaAtualAntesDeRedefinirSenhaPropria() {
        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token": "user-token"}
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-3/reset-password"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withNoContent());

        service.alterarMinhaSenha(jwt("user-3", "usuario3"), new AlterarSenhaDTO("atual123", "nova123"));

        server.verify();
    }

    @Test
    void deveTraduzirConflitoDoKeycloakAoCriarUsuario() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-token"))
                .andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.criar(new UsuarioCreateDTO(
                "existente",
                "Usuario",
                "Existente",
                "existente@example.local",
                "segredo123",
                true,
                List.of("usuario")
        )))
                .isInstanceOf(IdentidadeException.class)
                .hasMessage("Usuário já existe ou há conflito no provedor de identidade.")
                .extracting("status")
                .isEqualTo(HttpStatus.CONFLICT);

        server.verify();
    }

    @Test
    void deveTraduzirFalhaDeAutenticacaoAdministrativaDoKeycloak() {
        server.expect(once(), requestTo("http://keycloak/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.meuPerfil(jwt("user-3", "usuario3")))
                .isInstanceOf(IdentidadeException.class)
                .hasMessage("Serviço de identidade indisponível. Tente novamente em instantes.")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void deveObterTokenAdministrativoComClientCredentialsQuandoSecretConfigurado() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakUsuarioService(
                new KeycloakProperties("http://keycloak", "stella", "stella-cli", "stella", "stella-api-admin", null, null, "secret-tecnico"),
                builder
        );

        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("client_id=stella-api-admin&grant_type=client_credentials&client_secret=secret-tecnico"))
                .andRespond(withSuccess("""
                        {"access_token": "service-account-token"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-3"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-account-token"))
                .andRespond(withSuccess("""
                        {
                          "id": "user-3",
                          "username": "usuario3",
                          "enabled": true
                        }
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string("client_id=stella-api-admin&grant_type=client_credentials&client_secret=secret-tecnico"))
                .andRespond(withSuccess("""
                        {"access_token": "service-account-token"}
                        """, MediaType.APPLICATION_JSON));

        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-3/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-account-token"))
                .andRespond(withSuccess("""
                        [{"id": "role-usuario", "name": "usuario"}]
                        """, MediaType.APPLICATION_JSON));

        UsuarioResponseDTO usuario = service.buscarPorId("user-3");

        assertThat(usuario.username()).isEqualTo("usuario3");
        assertThat(usuario.roles()).containsExactly("usuario");
        server.verify();
    }

    private void expectUsuarioCompleto(String id, String username) {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/" + id))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "%s",
                          "username": "%s",
                          "firstName": "Novo",
                          "lastName": "Usuario",
                          "email": "novo@example.local",
                          "enabled": true
                        }
                        """.formatted(id, username), MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/" + id + "/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id": "role-usuario", "name": "usuario"}]
                        """, MediaType.APPLICATION_JSON));
    }

    private void expectAdminToken() {
        server.expect(once(), requestTo("http://keycloak/realms/master/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"access_token": "admin-token"}
                        """, MediaType.APPLICATION_JSON));
    }

    private org.springframework.security.oauth2.jwt.Jwt jwt(String subject, String username) {
        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .claim("preferred_username", username)
                .issuer("http://keycloak/realms/stella")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .jti(UUID.randomUUID().toString())
                .build();
    }
}
