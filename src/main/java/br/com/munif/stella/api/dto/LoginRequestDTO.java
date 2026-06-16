package br.com.munif.stella.api.dto;

/**
 * Authentication request DTO.
 *
 * <p>Contains the credentials provided by the user to obtain an access token.
 * The credentials are validated by the configured identity provider.</p>
 *
 * @param username username or email of the user
 * @param password user's plaintext password (transmitted via HTTPS)
 */
public record LoginRequestDTO(
        String username,
        String password
) {
}
