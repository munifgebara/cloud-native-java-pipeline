# System Overview

## Product Context

Stella manages personal inventory information and related operational data. The product currently focuses on authenticated access and inventory administration, with planned expansion for richer item cataloging, images, semantic search and AI-assisted workflows.

## Main Capabilities

- Authenticate users through Keycloak.
- Protect frontend routes and backend APIs with OAuth2/OIDC.
- Manage people and inventory-related entities.
- Store relational application state in PostgreSQL.
- Store item and location images in MinIO.
- Expose health and metrics through Spring Boot Actuator.
- Build and deploy through GitHub Actions and Kubernetes manifests.

## User Profiles

| Profile | Description |
| --- | --- |
| Administrator | Manages users, configuration and operational access. |
| Inventory owner | Maintains inventory items, locations and related images. |
| Basic user | Consults allowed inventory information and supported workflows. |

## Modules

| Module | Status | Notes |
| --- | --- | --- |
| Authentication | Implemented | Keycloak realm and OAuth2/OIDC integration. |
| People management | Implemented | Current primary business flow. |
| Inventory catalog | In progress | Items, instances, locations and images. |
| AI image analysis | In progress | OpenAI-backed assistance for item registration and image generation. |
| Semantic search | Planned | Vector database and embeddings strategy. |
| Observability | In progress | Metrics exist; log aggregation is planned. |

## Product Boundaries

Stella owns inventory application data and orchestration of its integrations. It does not own identity storage, object storage internals, log aggregation infrastructure, or external LLM model behavior.
