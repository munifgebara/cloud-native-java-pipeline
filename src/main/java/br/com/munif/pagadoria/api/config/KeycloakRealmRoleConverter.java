package br.com.munif.pagadoria.api.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return authorities;
        }

        Object rolesObject = realmAccess.get("roles");
        if (!(rolesObject instanceof Collection<?> roles)) {
            return authorities;
        }

        for (Object role : roles) {
            if (role instanceof String roleName && !roleName.isBlank()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }

        return authorities;
    }
}