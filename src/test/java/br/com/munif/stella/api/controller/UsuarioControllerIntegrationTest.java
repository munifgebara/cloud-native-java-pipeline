package br.com.munif.stella.api.controller;

import br.com.munif.stella.api.dto.MeuPerfilResponseDTO;
import br.com.munif.stella.api.dto.UsuarioCreateDTO;
import br.com.munif.stella.api.exception.IdentidadeException;
import br.com.munif.stella.api.service.KeycloakUsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class UsuarioControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @MockitoBean
    private KeycloakUsuarioService usuarioService;

    @Test
    void deveRetornarMeuPerfilSemErroInterno() throws Exception {
        when(usuarioService.meuPerfil(any(Jwt.class))).thenReturn(new MeuPerfilResponseDTO(
                "user-1",
                "usuario",
                "Usuario",
                "Stella",
                "usuario@example.local",
                List.of("usuario"),
                "http://keycloak/realms/stella/account"
        ));

        mockMvc.perform(get("/api/v0/usuarios/me")
                        .with(jwt().jwt(token -> token
                                .subject("user-1")
                                .claim("preferred_username", "usuario")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.username").value("usuario"));
    }

    @Test
    void deveTraduzirFalhaDoKeycloakNoMeuPerfilSemErroInterno() throws Exception {
        when(usuarioService.meuPerfil(any(Jwt.class))).thenThrow(new IdentidadeException(
                HttpStatus.BAD_GATEWAY,
                "Serviço de identidade indisponível. Tente novamente em instantes.",
                null
        ));

        mockMvc.perform(get("/api/v0/usuarios/me")
                        .with(jwt().jwt(token -> token
                                .subject("user-1")
                                .claim("preferred_username", "usuario")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.erro").value("Serviço de identidade indisponível. Tente novamente em instantes."));
    }

    @Test
    void deveTraduzirConflitoDoKeycloakNoCadastroSemErroInterno() throws Exception {
        when(usuarioService.criar(any(UsuarioCreateDTO.class))).thenThrow(new IdentidadeException(
                HttpStatus.CONFLICT,
                "Usuário já existe ou há conflito no provedor de identidade.",
                null
        ));

        UsuarioCreateDTO dto = new UsuarioCreateDTO(
                "existente",
                "Usuario",
                "Existente",
                "existente@example.local",
                "segredo123",
                true,
                List.of("usuario")
        );

        mockMvc.perform(post("/api/v0/usuarios")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.erro").value("Usuário já existe ou há conflito no provedor de identidade."));
    }
}
