# Architecture

> Part of the [Software Design Document](README.md). See also the official
> [Architecture guide](../architecture.md) and the
> [Build From Scratch guide](../build-from-scratch/README.md).

## High-Level View

Stella is a full-stack web application: an Angular single-page application (SPA) is served by,
and talks **only to**, a secured Spring Boot API. The API is the single integration point for
PostgreSQL, MinIO, Keycloak, the embeddings service and OpenAI.

> **Important:** the SPA never contacts Keycloak directly. Authentication is *backend-mediated*
> (see [Security](07-security.md)).

```mermaid
flowchart LR
    User([User Browser])
    subgraph Stella
        Frontend[Angular SPA<br/>served at /app]
        API[Spring Boot API<br/>:8080]
    end
    KC[Keycloak<br/>OIDC / JWT]
    PG[(PostgreSQL<br/>+ pgvector)]
    MINIO[(MinIO<br/>object storage)]
    EMB[Embeddings service<br/>MiniLM 384d]
    OPENAI[OpenAI API<br/>external · optional]
    OBS[[Prometheus · Loki · Grafana]]

    User --> Frontend
    Frontend -->|REST + Bearer JWT| API
    API -->|token exchange + JWKS| KC
    API --> PG
    API --> MINIO
    API --> EMB
    API -.-> OPENAI
    API -. metrics + logs .-> OBS
```

## Architectural Style

The backend follows conventional Spring layering:

- controllers expose HTTP endpoints (`/api/v0/...`, plus `/api/public/...` for unauthenticated reads and login)
- services coordinate business rules and integrations
- repositories access persistence; a shared `SuperRepository`/`SuperService` base centralizes CRUD, soft delete and audit-history queries
- DTOs define API contracts (entities are never exposed directly)
- configuration classes isolate framework and integration setup

The frontend is an Angular application organized around routed screens, a core auth/i18n layer,
services and reusable design-system components.

## C4 — Context

```mermaid
flowchart TB
    user([Person managing a personal inventory])
    admin([Administrator])
    stella[[Stella<br/>personal inventory platform]]
    kc[Keycloak<br/>identity provider]
    openai[OpenAI<br/>vision + image generation]
    obs[Observability platform<br/>Prometheus · Loki · Grafana]

    user --> stella
    admin --> stella
    stella -->|authenticates via| kc
    stella -.->|optional AI calls| openai
    stella -->|metrics + logs| obs
```

## C4 — Container

```mermaid
flowchart TB
    user([User Browser])
    spa[Angular SPA<br/>TypeScript · PrimeNG]
    api[Spring Boot API<br/>Java 25 · resource server]
    pg[(PostgreSQL 17<br/>+ pgvector)]
    minio[(MinIO<br/>S3-compatible)]
    emb[Embeddings service<br/>sentence-transformers]
    kc[Keycloak]
    openai[OpenAI API]

    user --> spa
    spa -->|HTTPS REST + JWT| api
    api -->|JDBC| pg
    api -->|S3 API| minio
    api -->|HTTP| emb
    api -->|token grant + JWKS| kc
    api -.->|HTTPS| openai
```

## Technology Baseline

| Area | Technology |
| --- | --- |
| Backend | Java 25, Spring Boot 4, Spring Security, Spring Data JPA, Flyway, Hibernate Envers |
| Frontend | Angular 21, TypeScript, PrimeNG, custom design system |
| Identity | Keycloak, OAuth2, OpenID Connect, JWT (backend-mediated password grant) |
| Data | PostgreSQL 17 with pgvector |
| Object storage | MinIO |
| AI | OpenAI (vision + image generation); local embeddings sidecar (MiniLM, 384 dims) — implemented |
| Deployment | Docker, Kubernetes (k3s), GitHub Actions, GHCR, Cloudflare Tunnel + Traefik ingress |
| Observability | Actuator/Micrometer, kube-prometheus-stack, Loki + Promtail, Grafana dashboards and alerts |

## Diagram Guidance

Use Mermaid for all diagrams so they render directly on GitHub. Keep diagrams close to the
implemented system and update them in the same pull request that changes the design.
