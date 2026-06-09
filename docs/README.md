# Stella Official Documentation

This directory is the official technical documentation for Stella. It complements the root `README.md` with operational and contributor-oriented guides.

## Guides

- [Architecture](architecture.md)
- [Local Development](local-development.md)
- [Configuration Reference](configuration.md)
- [Testing and Quality](testing.md)
- [Kubernetes Deployment](deployment.md)
- [Operations](operations.md)

## Project Summary

Stella is a cloud-native personal inventory system built with Spring Boot, Angular, Keycloak, PostgreSQL, MinIO, Docker Compose, Kubernetes and GitHub Actions.

The current application includes authenticated access, user management through Keycloak, people registration, inventory entities, image storage for catalog objects and storage locations, actuator metrics, structured server logs, and Kubernetes deployment assets.

## Documentation Principles

- Keep commands executable from the repository root unless stated otherwise.
- Prefer environment variables for deploy-specific values.
- Do not document real secrets, tokens or production credentials.
- Keep local defaults development-only.
- Update these guides when changing architecture, configuration, deployment, or operational behavior.
