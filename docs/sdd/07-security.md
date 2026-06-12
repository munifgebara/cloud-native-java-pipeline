# Security

## Authentication

Stella uses Keycloak as the identity provider. The frontend authenticates users through OAuth2/OpenID Connect, and the backend validates JWT access tokens as a resource server.

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
