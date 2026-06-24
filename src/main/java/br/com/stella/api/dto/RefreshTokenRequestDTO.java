package br.com.stella.api.dto;

/**
 * Refresh-token request DTO.
 *
 * @param refreshToken token issued by Keycloak to renew the access token
 */
public record RefreshTokenRequestDTO(
        String refreshToken
) {
}
