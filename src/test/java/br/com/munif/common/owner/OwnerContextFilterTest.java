package br.com.munif.common.owner;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnerContextFilterTest {

    @AfterEach
    void clearContexts() {
        OwnerContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldClearThreadLocalWhenTheRequestFails() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("user")
                .claim("email", " Owner@Example.Local ")
                .issuer("https://issuer.example/realms/stella")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        OwnerContextFilter filter = new OwnerContextFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        assertThatThrownBy(() -> filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            assertThat(OwnerContext.currentRequired().email()).isEqualTo("owner@example.local");
            throw new ServletException("request failed");
        })).isInstanceOf(ServletException.class);

        assertThat(OwnerContext.current()).isEmpty();
    }
}
