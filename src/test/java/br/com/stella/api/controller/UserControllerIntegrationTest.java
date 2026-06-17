package br.com.stella.api.controller;

import br.com.stella.api.dto.MyProfileResponseDTO;
import br.com.stella.api.dto.UserCreateDTO;
import br.com.stella.api.exception.IdentityException;
import br.com.stella.api.service.KeycloakUserService;
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
    private KeycloakUserService usuarioService;

    @Test
    void shouldReturnMeuProfileWithoutErrorInternal() throws Exception {
        when(usuarioService.myProfile(any(Jwt.class))).thenReturn(new MyProfileResponseDTO(
                "user-1",
                "user",
                "User",
                "Stella",
                "user@example.location",
                List.of("user"),
                "http://keycloak/realms/stella/account"
        ));

        mockMvc.perform(get("/api/v0/users/me")
                        .with(jwt().jwt(token -> token
                                .subject("user-1")
                                .claim("preferred_username", "user")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    void shouldTranslateFailureOfKeycloakInMeuProfileWithoutErrorInternal() throws Exception {
        when(usuarioService.myProfile(any(Jwt.class))).thenThrow(new IdentityException(
                HttpStatus.BAD_GATEWAY,
                "Identity service unavailable. Please try again in a moment.",
                null
        ));

        mockMvc.perform(get("/api/v0/users/me")
                        .with(jwt().jwt(token -> token
                                .subject("user-1")
                                .claim("preferred_username", "user")
                        ))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.error").value("Identity service unavailable. Please try again in a moment."));
    }

    @Test
    void shouldTranslateConflictOfKeycloakInRegistrationWithoutErrorInternal() throws Exception {
        when(usuarioService.create(any(UserCreateDTO.class))).thenThrow(new IdentityException(
                HttpStatus.CONFLICT,
                "User already exists or there is a conflict in the identity provider.",
                null
        ));

        UserCreateDTO dto = new UserCreateDTO(
                "existente",
                "User",
                "Existente",
                "existente@example.location",
                "segredo123",
                true,
                List.of("user")
        );

        mockMvc.perform(post("/api/v0/users")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("User already exists or there is a conflict in the identity provider."));
    }
}
