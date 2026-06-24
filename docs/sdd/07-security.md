# Security

> Part of the [Software Design Document](README.md). See also
> [Architecture](03-architecture.md) and the [Configuration reference](../configuration.md).

## Authentication

Stella uses Keycloak as the identity provider. Two login paths are supported:

- username/password remains backend-mediated through `POST /api/public/login`, with silent renewal
  through `POST /api/public/refresh`;
- social login redirects the browser to Keycloak using Authorization Code + PKCE, then Keycloak
  brokers the login with Google or GitHub and returns tokens to the SPA callback.

Every later request to `/api/v0/**` carries the access token as a Bearer header; an Angular HTTP
interceptor attaches it automatically and refreshes once before retrying a `401`. The backend
validates the JWT signature on each request as a resource server, against the configured issuer's
public keys (JWKS).

```mermaid
sequenceDiagram
    actor U as User
    participant SPA as Angular SPA
    participant API as Spring Boot API
    participant KC as Keycloak

    U->>SPA: enters username + password
    SPA->>API: POST /api/public/login
    API->>KC: token request (grant_type=password)
    KC-->>API: access + refresh tokens (JWT)
    API-->>SPA: tokens
    Note over SPA: stores tokens in localStorage

    U->>SPA: opens a protected screen
    SPA->>API: GET /api/v0/... (Authorization: Bearer JWT)
    API->>API: validate signature via cached Keycloak JWKS
    API-->>SPA: 200 + data

    SPA->>API: POST /api/public/refresh
    API->>KC: token request (grant_type=refresh_token)
    KC-->>API: renewed tokens
    API-->>SPA: renewed tokens
```

For social login, the browser follows the OIDC redirect flow directly with Keycloak. New brokered
users receive the realm default `usuario` role and the provider avatar is mapped to the `picture`
claim when the provider supplies it. OAuth app secrets are never stored in Git.

## Authorization

Authorization is enforced on the backend; Angular route guards only improve UX and are not a
security boundary. Current rules, as implemented:

- `/api/public/**` — open (login and public image reads).
- `/api/v0/**` — requires a valid JWT (`authenticated()`).
- `/api/v0/users/**` administrative operations — restricted with `@PreAuthorize("hasRole('admin')")`.
- Domain resources (items, instances, loans, locations, people) — currently **authenticated-only**,
  with no per-user ownership check. This is the single-tenant gap tracked by the planned
  [per-user data ownership](05-data-model.md#data-ownership-planned) feature.

Roles come from the Keycloak realm and are mapped from the `realm_access.roles` JWT claim by a
custom converter.

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
