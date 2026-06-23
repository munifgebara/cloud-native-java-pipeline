package br.com.stella.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class ActuatorHealthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnStatusUpInActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldAllowKubernetesHealthProbesWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        mockMvc.perform(get("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void shouldRequireAuthenticationForMetricsEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/prometheus")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAuthenticatedAccessToMetricsEndpoints() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
