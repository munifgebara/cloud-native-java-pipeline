package br.com.munif.stella.api.service;

import br.com.munif.stella.api.config.KeycloakProperties;
import br.com.munif.stella.api.dto.AlterarSenhaDTO;
import br.com.munif.stella.api.dto.MeuPerfilUpdateDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.dto.UsuarioResponseDTO;
import br.com.munif.stella.api.dto.UsuarioUpdateDTO;
import br.com.munif.stella.api.exception.IdentidadeException;
import br.com.munif.stella.api.exception.IntegracaoExternaException;
import jakarta.persistence.EntityNotFoundException;
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
import java.util.Map;
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

        assertThatThrownBy(() -> service.listar())
                .isInstanceOf(IdentidadeException.class)
                .hasMessage("Serviço de identidade indisponível. Tente novamente em instantes.")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);

        server.verify();
    }

    @Test
    void deveCarregarMeuPerfilAPartirDoJwtSemConsultarKeycloakAdmin() {
        var perfil = service.meuPerfil(jwt("user-7", "perfil7"));

        assertThat(perfil.id()).isEqualTo("user-7");
        assertThat(perfil.username()).isEqualTo("perfil7");
        assertThat(perfil.firstName()).isEqualTo("Nome");
        assertThat(perfil.lastName()).isEqualTo("Sobrenome");
        assertThat(perfil.email()).isEqualTo("perfil7@example.local");
        assertThat(perfil.roles()).containsExactly("admin", "usuario");
        assertThat(perfil.alteracaoSenhaUrl()).isEqualTo("http://keycloak/realms/stella/account");
        server.verify();
    }

    @Test
    void deveTraduzirFalhaDeConexaoComKeycloakComoServicoIndisponivel() {
        KeycloakUsuarioService servicoIndisponivel = new KeycloakUsuarioService(
                new KeycloakProperties("http://127.0.0.1:1", "stella", "stella-cli", "master", "admin-cli", "admin", "admin", null),
                RestClient.builder()
        );

        assertThatThrownBy(servicoIndisponivel::listar)
                .isInstanceOf(IdentidadeException.class)
                .hasMessage("Serviço de identidade indisponível. Tente novamente em instantes.")
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_GATEWAY);
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

    @Test
    void deveAtualizarUsuarioSubstituindoRolesGerenciadas() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-4",
                          "username": "editor",
                          "firstName": "Nome",
                          "lastName": "Antigo",
                          "email": "antigo@example.local",
                          "enabled": true
                        }
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {
                          "id": "user-4",
                          "username": "editor",
                          "firstName": "Nome",
                          "lastName": "Atualizado",
                          "email": null,
                          "enabled": false
                        }
                        """))
                .andRespond(withNoContent());

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id": "role-admin", "name": "admin"},
                          {"id": "role-default", "name": "default-roles-stella"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4/role-mappings/realm"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/roles/usuario"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id": "role-usuario", "name": "usuario"}
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4/role-mappings/realm"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withNoContent());

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-4",
                          "username": "editor",
                          "firstName": "Nome",
                          "lastName": "Atualizado",
                          "enabled": false
                        }
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-4/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"id": "role-usuario", "name": "usuario"},
                          {"id": "role-default", "name": "default-roles-stella"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        UsuarioResponseDTO usuario = service.atualizar("user-4", new UsuarioUpdateDTO(
                " Nome ",
                " Atualizado ",
                " ",
                false,
                List.of("usuario", "default-roles-stella")
        ));

        assertThat(usuario.enabled()).isFalse();
        assertThat(usuario.email()).isNull();
        assertThat(usuario.roles()).containsExactly("usuario");
        server.verify();
    }

    @Test
    void deveAtualizarMeuPerfilNormalizandoCamposEInformandoUrlDaConta() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-5",
                          "username": "perfil",
                          "firstName": "Antigo",
                          "lastName": "Nome",
                          "email": "antigo@example.local",
                          "enabled": true
                        }
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-5"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("""
                        {
                          "id": "user-5",
                          "username": "perfil",
                          "firstName": "Novo",
                          "lastName": null,
                          "email": "novo@example.local",
                          "enabled": true
                        }
                        """))
                .andRespond(withNoContent());

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "id": "user-5",
                          "username": "perfil",
                          "firstName": "Novo",
                          "email": "novo@example.local",
                          "enabled": true
                        }
                        """, MediaType.APPLICATION_JSON));

        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/user-5/role-mappings/realm"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id": "role-admin", "name": "admin"}]
                        """, MediaType.APPLICATION_JSON));

        var perfil = service.atualizarMeuPerfil(jwt("user-5", "perfil"), new MeuPerfilUpdateDTO(
                " Novo ",
                " ",
                " novo@example.local "
        ));

        assertThat(perfil.firstName()).isEqualTo("Novo");
        assertThat(perfil.lastName()).isNull();
        assertThat(perfil.alteracaoSenhaUrl()).isEqualTo("http://keycloak/realms/stella/account");
        assertThat(perfil.roles()).containsExactly("admin");
        server.verify();
    }

    @Test
    void deveTraduzirUsuarioNaoEncontradoParaEntidadeNaoEncontrada() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users/desconhecido"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> service.buscarPorId("desconhecido"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Usuário não encontrado.");

        server.verify();
    }

    @Test
    void deveRejeitarAlteracaoDeSenhaQuandoSenhaAtualForInvalida() {
        server.expect(once(), requestTo("http://keycloak/realms/stella/protocol/openid-connect/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> service.alterarMinhaSenha(jwt("user-6", "usuario6"), new AlterarSenhaDTO("errada", "nova123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Senha atual inválida.");

        server.verify();
    }

    @Test
    void deveFalharQuandoCredenciaisAdministrativasNaoEstaoConfiguradas() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        service = new KeycloakUsuarioService(
                new KeycloakProperties("http://keycloak", "stella", "stella-cli", "master", "admin-cli", null, " ", null),
                builder
        );

        assertThatThrownBy(() -> service.listar())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Credenciais administrativas do Keycloak não configuradas.");

        server.verify();
    }

    @Test
    void deveFalharQuandoKeycloakNaoRetornaLocationAoCriarUsuario() {
        expectAdminToken();
        server.expect(once(), requestTo("http://keycloak/admin/realms/stella/users"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> service.criar(new UsuarioCreateDTO(
                "sem-location",
                "Sem",
                "Location",
                "sem-location@example.local",
                "segredo123",
                true,
                List.of("usuario")
        )))
                .isInstanceOf(IntegracaoExternaException.class)
                .hasMessage("Keycloak não retornou o identificador do usuário criado.");

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
                .claim("given_name", "Nome")
                .claim("family_name", "Sobrenome")
                .claim("email", username + "@example.local")
                .claim("realm_access", Map.of("roles", List.of("offline_access", "usuario", "admin")))
                .issuer("http://keycloak/realms/stella")
                .issuedAt(java.time.Instant.now())
                .expiresAt(java.time.Instant.now().plusSeconds(300))
                .jti(UUID.randomUUID().toString())
                .build();
    }
}
