package br.com.munif.stella.api.dto;

/**
 * DTO de requisição de autenticação.
 *
 * <p>Contém as credenciais fornecidas pelo usuário para obter um token de acesso.
 * As credenciais são validadas pelo provedor de identidade configurado.</p>
 *
 * @param username nome de usuário ou e-mail do usuário
 * @param password senha do usuário em texto plano (transmitida via HTTPS)
 */
public record LoginRequestDTO(
        String username,
        String password
) {
}
