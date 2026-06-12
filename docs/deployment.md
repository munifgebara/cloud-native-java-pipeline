# Kubernetes Deployment

## Manifests

Kubernetes assets live under `k8s/platform/`:

| Path | Purpose |
| --- | --- |
| `namespaces.yaml` | Platform namespace |
| `postgres/` | PostgreSQL stateful workload and service |
| `keycloak/` | Keycloak deployment, service and realm config |
| `minio/` | MinIO deployment, service and persistent volume claim |
| `observability/` | Grafana datasource and dashboard ConfigMaps for the existing Gimli logging stack |
| `stella-api/` | Stella API deployment, service, ingress and configuration |

## CI/CD Flow

The repository uses three GitHub Actions workflows:

| Workflow | Responsibility |
| --- | --- |
| `ci.yml` | Runs `./mvnw clean verify` for pushes and pull requests |
| `publish-stella-api.yml` | Builds and publishes the API image to GHCR after CI succeeds on `main` |
| `cd.yml` | Applies Kubernetes manifests and updates the API image on the self-hosted k3s runner |

The published image is tagged as `latest`, `main`, and the commit SHA.

## Applying Manifests Manually

From the repository root:

```bash
sudo k3s kubectl apply -R -f k8s/platform/
```

Update the API image manually:

```bash
sudo k3s kubectl set image deployment/stella-api \
  stella-api=ghcr.io/munifgebara/stella-api:<commit-sha> \
  -n platform
```

Wait for rollout:

```bash
sudo k3s kubectl rollout status deployment/stella-api -n platform --timeout=180s
```

## Required Server Configuration

The API deployment reads non-sensitive values from `stella-api-configmap.yaml` and sensitive values from Kubernetes secrets.

Required production-sensitive values include:

- database credentials
- Keycloak admin client secret
- MinIO credentials
- registry pull secret when using private GHCR images

The server profile is activated with:

```text
SPRING_PROFILES_ACTIVE=server
```

In this profile, logs are emitted as ECS JSON to stdout for collection by the cluster logging stack.

## Post-Deploy Checks

```bash
sudo k3s kubectl get pods -n platform
sudo k3s kubectl rollout status deployment/stella-api -n platform
sudo k3s kubectl logs deployment/stella-api -n platform --tail=50
```

Application checks:

- `/actuator/health` should report healthy readiness for the API.
- `/app` should serve the integrated Angular application.
- `/scalar` should expose the API documentation UI.
