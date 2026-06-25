package br.com.stella.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SpaForwardControllerTest {

    private final MockMvc mockMvc = standaloneSetup(new SpaForwardController()).build();

    @Test
    void forwardsSpaCallbackRouteToAngularIndex() throws Exception {
        mockMvc.perform(get("/app/auth/callback"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/app/index.html"));
    }

    @Test
    void doesNotForwardStaticAssetsWithExtensions() throws Exception {
        mockMvc.perform(get("/app/main.js"))
                .andExpect(status().isNotFound());
    }
}
