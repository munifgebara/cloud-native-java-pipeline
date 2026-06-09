# Operations

## Health and Metrics

Useful API endpoints:

- `/actuator/health`
- `/actuator/metrics`
- `/actuator/prometheus`
- `/scalar`

In Kubernetes, start with:

```bash
sudo k3s kubectl get pods -n platform
sudo k3s kubectl describe pod -n platform -l app=stella-api
sudo k3s kubectl logs deployment/stella-api -n platform --tail=100
```

## Logs

Local runs use readable console logs. Kubernetes runs should use the `server` profile, which writes ECS JSON logs to stdout.

Stella does not push logs directly to Grafana. The expected production path is:

```text
Stella pod stdout -> cluster log collector -> Loki or equivalent -> Grafana
```

Valid collectors include Grafana Alloy, Promtail, Fluent Bit, or another Kubernetes log collector.

Avoid logging:

- tokens
- passwords
- client secrets
- full personal-data payloads
- SQL bind values in production

## Common Checks

Check the deployed image:

```bash
sudo k3s kubectl get deployment stella-api -n platform \
  -o jsonpath='{.spec.template.spec.containers[0].image}'
```

Check API configuration values from the ConfigMap:

```bash
sudo k3s kubectl get configmap stella-api-config -n platform -o yaml
```

Check rollout history:

```bash
sudo k3s kubectl rollout history deployment/stella-api -n platform
```

## Backup Notes

PostgreSQL and MinIO hold durable application data. Backups should cover both:

- PostgreSQL database dump or volume snapshot
- MinIO bucket contents or persistent volume snapshot

Backups are not fully automated in the current repository and should be treated as a separate operational hardening item.
