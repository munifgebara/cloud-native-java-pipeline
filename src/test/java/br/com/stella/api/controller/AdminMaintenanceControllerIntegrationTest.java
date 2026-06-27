package br.com.stella.api.controller;

import br.com.stella.api.dto.InactiveRecordsPurgeResultDTO;
import br.com.stella.api.service.InactiveRecordsPurgeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class AdminMaintenanceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InactiveRecordsPurgeService service;

    @Test
    void shouldRejectUnauthenticatedPurge() throws Exception {
        mockMvc.perform(delete("/api/v0/admin/maintenance/inactive-records"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectPurgeWithoutAdminRole() throws Exception {
        mockMvc.perform(delete("/api/v0/admin/maintenance/inactive-records").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToPurgeInactiveRecords() throws Exception {
        when(service.purge()).thenReturn(new InactiveRecordsPurgeResultDTO(1, 2, 3, 4, 5, 6, 7, 8));

        mockMvc.perform(delete("/api/v0/admin/maintenance/inactive-records")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemLoans").value(1))
                .andExpect(jsonPath("$.total").value(36));

        verify(service).purge();
    }
}
