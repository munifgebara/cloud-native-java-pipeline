# Stella

English | [Portuguese (pt-BR)](README.pt-BR.md)

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

Today, the main implemented business flow is the management of people (`pessoas`) within the personal inventory context, alongside login, route protection, and dashboard basics. The repository structure and infrastructure already prepare the project for expanding into broader inventory modules such as item ownership, categorization, and tracking.

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

Authentication flow
  -> User accesses the SPA
  -> SPA redirects the user to Keycloak (:9080)
  -> Keycloak authenticates and issues tokens
  -> SPA calls the API with a bearer token
  -> API validates the JWT and processes the request
```

## Tech Stack

| Layer | Technology |
| --- | --- |
| Backend | Spring Boot 4, Spring Security, Spring Data JPA, Flyway, Actuator |
| Frontend | Angular 21, PrimeNG, TypeScript |
| Identity | Keycloak, OAuth2, OpenID Connect, JWT |
| Database | PostgreSQL |
| Observability | Micrometer, Prometheus endpoint |
| Infra | Docker Compose, Kubernetes |
| CI/CD | GitHub Actions, GHCR |

## Current Functional Scope

Implemented or already visible in the codebase:

- login flow integrated with Keycloak
- protected Angular routes
- people listing and editing screens
- Spring Boot REST API secured as a resource server
- database migrations with Flyway
- Docker-based local environment
- Kubernetes deployment assets
- CI/CD workflow foundation
- actuator and Prometheus metrics exposure

Planned evolution visible in the backlog:

- broader inventory modules
- internationalization
- improved logging and observability integration
- CEP-based address autofill
- documentation and onboarding refinements

## Repository Structure

```text
.
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


## Roadmap de Implementação do Módulo de Inventário

A implementação deve priorizar sempre a menor fase aberta e, dentro dela, as issues marcadas como `ready`.

### Ordem Recomendada

1. `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
2. `#27` Criar cadastro de item mestre do inventário
3. `#31` Criar cadastro de instâncias de item
4. `#33` Definir status operacional das instâncias de item
5. `#32` Criar listagem e filtros de itens e instâncias
6. `#34` Implementar movimentações de entrada de instâncias
7. `#35` Implementar movimentações de saída de instâncias
8. `#36` Implementar transferência de instâncias entre locais
9. `#38` Implementar empréstimo de instâncias para pessoas
10. `#39` Implementar devolução de instâncias emprestadas
11. `#37` Criar histórico consolidado da instância do item
12. `#40` Validar regras de negócio de inventário, movimentação e empréstimo
13. `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
14. `#30` Adicionar imagem principal ao cadastro de item mestre
15. `#41` Expandir o dashboard com indicadores do inventário
16. `#28` Adicionar ícones predefinidos ao cadastro de categorias
17. `#42` Adicionar testes automatizados ao módulo de inventário

### Implementação por Fases

#### Fase 1 - Núcleo do Inventário
- `#26` Criar cadastro de locais hierárquicos para armazenagem dos itens
- `#27` Criar cadastro de item mestre do inventário
- `#31` Criar cadastro de instâncias de item
- `#33` Definir status operacional das instâncias de item
- `#32` Criar listagem e filtros de itens e instâncias

#### Fase 2 - Movimentações
- `#34` Implementar movimentações de entrada de instâncias
- `#35` Implementar movimentações de saída de instâncias
- `#36` Implementar transferência de instâncias entre locais

#### Fase 3 - Empréstimos e Regras
- `#38` Implementar empréstimo de instâncias para pessoas
- `#39` Implementar devolução de instâncias emprestadas
- `#37` Criar histórico consolidado da instância do item
- `#40` Validar regras de negócio de inventário, movimentação e empréstimo

#### Fase 4 - Mídia com MinIO
- `#29` Integrar o Stella ao MinIO para armazenamento de imagens dos itens
- `#30` Adicionar imagem principal ao cadastro de item mestre

#### Fase 5 - Dashboard e Qualidade
- `#41` Expandir o dashboard com indicadores do inventário
- `#28` Adicionar ícones predefinidos ao cadastro de categorias
- `#42` Adicionar testes automatizados ao módulo de inventário
