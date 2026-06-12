# Deployment

## Environments

| Environment | Purpose |
| --- | --- |
| Local | Developer machine with Docker Compose and local defaults. |
| CI | GitHub Actions build and verification environment. |
| Server | Kubernetes/K3S deployment using server profile configuration. |

## Docker

Docker Compose provides local infrastructure for PostgreSQL, Keycloak and MinIO. The integrated build packages the backend and frontend for deployment.

## Kubernetes/K3S

Kubernetes manifests live under `k8s/platform/`. They define the platform namespace, database, identity provider, object storage and Stella API deployment.

## PostgreSQL

PostgreSQL stores durable relational state. Deployment design should define backup, restore and migration expectations before production use.

## MinIO

MinIO stores image objects. Deployment design should define bucket configuration, credentials, persistence and backup expectations.

## ConfigMaps and Secrets

- ConfigMaps should hold non-sensitive runtime values.
- Secrets should hold credentials, API keys and tokens.
- Secret names and required keys should be documented alongside deployment changes.

## CI/CD

GitHub Actions runs verification, publishes container images and applies deployment updates. The deployment design should keep manual recovery commands documented in `docs/deployment.md` and operational checks in `docs/operations.md`.
