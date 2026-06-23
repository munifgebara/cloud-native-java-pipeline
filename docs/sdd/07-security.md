# Security

## Authentication

Stella uses Keycloak as the identity provider. Authentication is **backend-mediated**: the SPA never contacts Keycloak directly. The frontend posts the user's credentials to the API, which exchanges them with Keycloak using the OAuth2 password grant and relays the resulting tokens back to the SPA. The backend then validates JWT access tokens on every request as a resource server (signature checked against the configured issuer's JWKS).

> Note: the password grant keeps the integration simple and didactic but is not the recommended browser flow for production. A future evolution may move to the Authorization Code flow with PKCE, where the browser is redirected to Keycloak directly.

## Authorization

Authorization should be enforced on the backend. Frontend route guards improve user experience but are not a security boundary.

Future updates should document:

- role definitions
- endpoint-level access rules
- domain-level ownership checks
- administrative operations

## Keycloak Integration

Keycloak owns users, credentials, realm configuration and token issuance. Production user-management operations should use a dedicated confidential client with the minimum required service-account roles.

## JWT, OAuth2 and OIDC

The backend trusts tokens only from the configured issuer. Token claims used for authorization should be documented when role and permission mapping becomes stable.

## Audit

Security-sensitive operations should be auditable. The audit strategy should define what is logged, where it is stored and how personal data is protected.

## LGPD

Design changes involving personal data should consider:

- data minimization
- purpose limitation
- retention
- access control
- deletion or anonymization needs

## OWASP Top 10

Security reviews should explicitly consider common risks such as broken access control, injection, insecure design, vulnerable dependencies, authentication failures and security logging gaps.

## Secrets

Secrets must not be committed to the repository. Runtime secrets should come from environment variables, Kubernetes Secrets or equivalent platform mechanisms.
