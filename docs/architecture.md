# Architecture

## Runtime View

```text
Browser
  -> Angular SPA served at /app
  -> Spring Boot API on port 8080
  -> PostgreSQL for relational state
  -> MinIO for uploaded images

Identity
  -> Keycloak issues OpenID Connect tokens
  -> Angular sends bearer tokens to the API
  -> Spring Security validates JWTs using the configured issuer
  -> User administration uses the Keycloak Admin REST API
```

## Main Components

| Component | Responsibility |
| --- | --- |
| Angular frontend | Authenticated UI, routing, inventory and administration screens |
| Spring Boot API | REST endpoints, validation, authorization, business rules and persistence |
| PostgreSQL | Durable relational data managed by Flyway migrations |
| Keycloak | Authentication, realm users, client configuration and JWT issuance |
| MinIO | S3-compatible object storage for images |
| Kubernetes manifests | Server deployment model for API, database, Keycloak and MinIO |
| GitHub Actions | CI, image publication and deployment automation |

## Backend Structure

The backend follows the usual Spring layering:

- `controller`: HTTP API surface
- `service`: business rules and integrations
- `repository`: persistence access
- `entity`: JPA entities
- `dto`: request and response contracts
- `config`: framework and integration configuration
- `exception`: API error handling

Database schema changes are versioned in `src/main/resources/db/migration` and applied by Flyway.

## Frontend Structure

The frontend lives in `frontend/` and is built as part of the Maven lifecycle. The production Angular bundle is copied to `src/main/resources/static/app`, allowing the Spring Boot application to serve the integrated UI at `/app`.

## Security Model

The API is an OAuth2 resource server. It validates access tokens issued by the configured Keycloak realm:

```yaml
spring.security.oauth2.resourceserver.jwt.issuer-uri:
  ${stella.keycloak.base-url}/realms/${stella.keycloak.realm}
```

Production user-management operations should use a dedicated confidential Keycloak client with service account enabled. The application falls back to local admin credentials only when `STELLA_KEYCLOAK_ADMIN_CLIENT_SECRET` is not configured.

## Observability Model

The API exposes actuator endpoints for health, metrics and Prometheus scraping. Local logs use a readable console format. The server profile emits structured ECS JSON logs to stdout so a Kubernetes log collector can forward them to Grafana/Loki or an equivalent platform.
