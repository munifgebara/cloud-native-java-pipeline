# Observability

## Logs

Local logs are optimized for readability. Server logs should be structured and written to stdout so the Kubernetes platform can collect them.

Expected production path:

```text
Stella pod stdout -> log collector -> log backend -> Grafana or equivalent
```

## Metrics

Spring Boot Actuator and Micrometer expose application metrics. Prometheus-compatible scraping is available through the actuator Prometheus endpoint.

## Tracing

Distributed tracing is not yet fully specified. Future design should define trace propagation, sampling and correlation IDs before introducing cross-service workflows.

## Monitoring

Initial monitoring should cover:

- API health
- request latency and error rate
- JVM health
- database connectivity
- MinIO connectivity
- external AI integration failures

## Alerts

Alerting is planned. Alert rules should focus on user impact and operational risk instead of noisy internal details.

## Future Integrations

Candidate tools include:

- OpenTelemetry for traces and metrics instrumentation
- Prometheus for metrics collection
- Grafana for dashboards
- Loki, ELK or OpenSearch for logs
- Grafana Alloy, Promtail or Fluent Bit for log collection
