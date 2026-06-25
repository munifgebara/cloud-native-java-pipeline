package br.com.munif.common.owner;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class OwnerContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            resolveIdentity().ifPresent(OwnerContext::set);
            filterChain.doFilter(request, response);
        } finally {
            OwnerContext.clear();
        }
    }

    private java.util.Optional<OwnerIdentity> resolveIdentity() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return java.util.Optional.empty();
        }
        if (!(authentication.getPrincipal() instanceof Jwt jwt)) {
            return java.util.Optional.empty();
        }

        String email = OwnerIdentity.normalizeEmail(jwt.getClaimAsString("email"));
        String issuer = jwt.getIssuer() == null ? null : OwnerIdentity.normalizeIssuer(jwt.getIssuer().toString());
        if (email == null || issuer == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new OwnerIdentity(email, issuer));
    }
}
