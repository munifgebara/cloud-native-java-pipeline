# Stella

English | [Português (pt-BR)](README.pt-BR.md) | [Español](README.es.md)

Stella is a cloud-native personal inventory management project built to demonstrate a full-stack Java platform with modern authentication, containerized local infrastructure, Kubernetes deployment, and CI/CD automation.

It was designed with two complementary goals:

- portfolio: present an end-to-end software engineering project with backend, frontend, infrastructure, security, and delivery concerns
- learning: offer a didactic example that students can use to understand how a SPA, a secured API, databases, containers, and deployment pipelines fit together

## Overview

The application combines:

- Spring Boot 4 API with Java 25
- Angular 21 SPA with PrimeNG
- Keycloak for OAuth2 / OpenID Connect authentication
- PostgreSQL with Flyway migrations
- Docker Compose for local infrastructure
- Kubernetes manifests for deployment
- GitHub Actions workflows for CI, image publishing, and deployment
- Prometheus-ready actuator metrics for observability

Today the application is a working personal inventory platform. Implemented flows include a hierarchical storage-location registry, master items and physical item instances, item movements and loans, people registration, semantic (vector) search over the catalog, AI-assisted registration from a photo and AI image generation, full change auditing (Hibernate Envers), and internationalization (pt-BR / en / es). Authentication, route protection, and dashboards are in place. The architecture is prepared for per-user data ownership, which is the next planned step (see [Data Ownership](#data-ownership-planned)).

> **New to the project?** Start with the didactic, end-to-end guide
> [Build a Similar Project From Scratch (Ubuntu Server)](docs/build-from-scratch/README.md),
> available in English, Portuguese, and Spanish, with architecture, data-model, and CI/CD
> diagrams.

## Why This Project Matters

Stella is intentionally broader than a CRUD demo. It shows how application code and platform concerns evolve together:

- authentication is externalized through Keycloak instead of being hardcoded in the app
- frontend and backend are integrated in a single delivery flow
- the application is packaged for container-based deployment
- Kubernetes manifests and GitHub Actions push the project toward a production-style workflow
- actuator and Prometheus support open the door for monitoring and operational maturity

This makes the repository useful both as a portfolio piece and as a teaching reference for students learning cloud-native Java development.

## Architecture

```text
Browser
  -> Angular SPA (/app)
  -> Spring Boot API (:8080)
  -> PostgreSQL (:5432)

Authentication flow (backend-mediated; the SPA never calls Keycloak directly)
  -> User submits username/password to the SPA
  -> SPA posts the credentials to the API
  -> API exchanges them with Keycloak (:9080) using the OAuth2 password grant
  -> Keycloak returns access/refresh tokens to the API
  -> API returns the tokens to the SPA
  -> SPA calls the API with a bearer token
  -> API validates the JWT signature (Keycloak JWKS) and processes the request
```

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Spring Boot 4, Spring Security, Spring Data JPA, Flyway, Hibernate Envers, Actuator |
| Frontend | Angular 21, PrimeNG, TypeScript, custom design system |
| Identity | Keycloak, OAuth2, OpenID Connect, JWT |
| Database | PostgreSQL 17 with pgvector (vector search) |
| Object storage | MinIO (S3-compatible) for item images |
| AI | OpenAI (photo analysis, image generation), local embeddings sidecar (MiniLM, 384 dims) |
| Observability | Micrometer, Prometheus rules + ServiceMonitor, Grafana/Loki structured logs |
| Infra | Docker Compose, Kubernetes (k3s) |
| CI/CD | GitHub Actions, GHCR, Trivy security scan |

## Current Functional Scope

Implemented and visible in the codebase:

- login flow integrated with Keycloak and protected Angular routes
- Spring Boot REST API secured as an OAuth2 resource server (JWT)
- hierarchical storage locations, categories, master items and item instances
- item movements (in/out/transfer) and loans to people
- image storage in MinIO, including AI-assisted registration from a photo and AI image generation
- semantic (vector) search over the catalog using pgvector and a local embeddings service
- full change auditing with Hibernate Envers
- internationalization (pt-BR / en / es) and a custom design system
- database migrations with Flyway and a clean English schema checkpoint
- Docker-based local environment and Kubernetes (k3s) deployment assets
- CI/CD workflows (build/test, image publish, deploy) and Trivy security scanning
- actuator metrics, Prometheus rules, ServiceMonitor and Grafana/Loki logging

Planned evolution visible in the backlog:

- per-user data ownership (horizontal authorization) — see [Data Ownership](#data-ownership-planned)
- global (cross-replica) AI usage limits and autoscaling (HPA)
- paginated listings and frontend infinite scroll for large inventories

## Repository Structure

```text
.
|-- docs/                      # Official project documentation
|-- frontend/                  # Angular SPA
|-- k8s/                       # Kubernetes manifests
|-- keycloak/                  # Realm import files
|-- postgres/                  # Database bootstrap scripts
|-- src/main/java/             # Spring Boot application code
|-- src/main/resources/        # Configuration, migrations, static assets
|-- .github/workflows/         # CI/CD pipelines
|-- docker-compose.yml         # Local infrastructure
`-- pom.xml                    # Maven build, frontend integration, tests
```

## Official Documentation

The official technical documentation is available in [`docs/`](docs/README.md):

- [Build a Similar Project From Scratch (Ubuntu Server)](docs/build-from-scratch/README.md) — EN / PT / ES, with diagrams
- [Architecture](docs/architecture.md)
- [Local Development](docs/local-development.md)
- [Configuration Reference](docs/configuration.md)
- [Testing and Quality](docs/testing.md)
- [Kubernetes Deployment](docs/deployment.md)
- [Operations](docs/operations.md)

## Local Development

### Prerequisites

- Java 25
- Maven Wrapper or Maven 3.9+
- Node.js 22+ and npm
- Docker and Docker Compose

### 1. Start infrastructure

```bash
docker compose up -d
```

This brings up:

- PostgreSQL on `127.0.0.1:5432`
- Keycloak on `http://127.0.0.1:9080`
- MinIO API on `http://127.0.0.1:9000`
- MinIO console on `http://127.0.0.1:9001`

Default local MinIO credentials are `minioadmin` / `minioadmin`. The backend uses bucket `stella-itens` by default and creates it automatically on the first image upload.

MinIO configuration can be overridden with:

| Variable | Default | Description |
| --- | --- | --- |
| `STELLA_MINIO_ENDPOINT` | `http://127.0.0.1:9000` | S3-compatible endpoint used by the backend |
| `STELLA_MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key and local root user |
| `STELLA_MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key and local root password |
| `STELLA_MINIO_BUCKET` | `stella-itens` | Bucket used for item images |
| `STELLA_MINIO_MAX_IMAGE_SIZE_BYTES` | `5242880` | Maximum accepted image size |

### 2. Run the backend

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API runs on `http://127.0.0.1:8080`.

### 3. Run the frontend in development mode

```bash
cd frontend
npm install
npm start
```

The Angular dev server runs on `http://127.0.0.1:4200`.

### 4. Build the integrated application

```bash
./mvnw clean verify
```

The Maven build installs frontend dependencies, builds the Angular app, and packages the backend.

### 5. Run BDD scenarios

```bash
./mvnw -Dtest=CucumberBddTest test
```

BDD scenarios are written in Gherkin under `src/test/resources/features`, with step definitions in `src/test/java`.

## Authentication and Demo Access

Local authentication is handled by Keycloak with the `stella` realm.

Default local admin credentials:

- username: `admin`
- password: `admin123`

Realm users available in the local bootstrap:

- `admin` - system administrator
- `proprietario` - inventory owner / primary manager of registered items
- `usuario` - user with basic access for consultation flows

JWT validation is configured through Spring Security as an OAuth2 resource server.

The user management module uses Keycloak Admin REST as the identity source. In production, prefer a dedicated confidential client with service account enabled instead of reusing the main Keycloak administrator account. The Stella API requests the administrative token with `client_credentials` whenever `STELLA_KEYCLOAK_ADMIN_CLIENT_SECRET` is configured.

Required production identity settings:

- `STELLA_KEYCLOAK_ADMIN_REALM`: realm that owns the technical client, normally `stella`
- `STELLA_KEYCLOAK_ADMIN_CLIENT_ID`: technical client id, normally `stella-api-admin`
- `STELLA_KEYCLOAK_ADMIN_CLIENT_SECRET`: Kubernetes secret value for the technical client

The technical client must have only the required `realm-management` roles for user operations in the `stella` realm, such as `manage-users`, `query-users`, `view-users` and `view-realm`.

For local development, if `STELLA_KEYCLOAK_ADMIN_CLIENT_SECRET` is not defined, the backend keeps the previous fallback and uses the local administrator configured in `docker-compose.yml`:

- `STELLA_KEYCLOAK_ADMIN_USERNAME`
- `STELLA_KEYCLOAK_ADMIN_PASSWORD`

## API and Observability

Useful local endpoints:

- application: `http://127.0.0.1:8080/app`
- API base: `http://127.0.0.1:8080/api`
- OpenAPI / Scalar UI: `http://127.0.0.1:8080/scalar`
- health: `http://127.0.0.1:8080/actuator/health`
- metrics: `http://127.0.0.1:8080/actuator/metrics`
- prometheus: `http://127.0.0.1:8080/actuator/prometheus`

### Logging by Environment

Local runs use the default Spring profile and keep human-readable logs on the application console. The default log level is controlled by `STELLA_LOG_LEVEL` and falls back to `INFO`.

The Kubernetes deployment activates the `server` profile through `SPRING_PROFILES_ACTIVE=server`. In this profile, Stella emits ECS structured JSON logs to stdout, which is the expected collection point for the Grafana/Loki stack or any cluster log collector. The deployment labels and annotations identify the service, component, destination and log format without requiring file-based logging.

Useful server logging variables:

- `STELLA_LOG_LEVEL`: root application log level
- `STELLA_SECURITY_LOG_LEVEL`: Spring Security log level
- `STELLA_ENVIRONMENT`: environment value included in structured logs

Do not log tokens, passwords, client secrets or personal data payloads. The application configuration keeps SQL parameter binding at `WARN` in server mode to avoid leaking sensitive values in logs.

## Deployment and Delivery Flow

The repository already includes the building blocks for a cloud-native delivery workflow:

- `ci.yml` validates the application on pushes and pull requests
- `publish-stella-api.yml` builds and publishes the container image
- `cd.yml` updates the Kubernetes deployment after a successful publish
- `k8s/` stores the manifests used in the cluster

This setup helps demonstrate the transition from local development to automated delivery.

## Learning Notes

Students and reviewers can use this repository to explore:

- how a Spring Boot API works as a JWT-protected resource server
- how Angular and Spring Boot can be delivered together
- how Flyway keeps database evolution explicit
- how Docker Compose simplifies local onboarding
- how GitHub Actions can separate CI, publish, and deploy responsibilities
- how observability concerns begin with metrics and structured operational thinking

## Roadmap

- expand domain coverage beyond the current people flow
- improve multilingual support in the UI
- refine server-side logging and Grafana integration
- strengthen documentation for contributors and students
- continue hardening the pipeline for production-like environments

## Author

Munif Gebara Junior

If you are evaluating this repository as a portfolio project, the strongest signals are the combination of application code, infrastructure, authentication, observability, and delivery workflow in a single learning-oriented system.

## Data Ownership (planned)

> **Status: planned — not yet implemented. The database is already prepared for it.**

The system is currently **single-tenant**: any authenticated user can see and modify all
inventory data. The next planned step is **per-user data ownership** (horizontal
authorization), so each user only sees their own records.

The shared base entity already provides an indexed `external_id` column on every business table
(for example, `ix_person_external_id` exists in the initial Flyway migration). This column is
the reserved slot intended to carry the **owner** — the Keycloak subject of the user who created
the record. The structure exists; the missing parts are the semantics (populating the owner from
the authenticated JWT) and the enforcement (scoping every read and write to the owner). Until
this feature ships, treat any deployment as single-tenant.

See the full description and diagram in
[Build a Similar Project From Scratch §10](docs/build-from-scratch/en.md#10-planned-feature-per-user-data-ownership).
