# Build a Similar Project From Scratch (Ubuntu Server)

A didactic, end-to-end walkthrough that takes a clean **Ubuntu Server** and turns it into a
running cloud-native stack like Stella — covering **two paths**:

- **Manual** — you, as an engineer, type every command.
- **Agent-assisted** — an AI coding agent (e.g. Claude Code) builds it under your review.

This guide is the same content in three languages:

| Language | File |
| --- | --- |
| 🇬🇧 English | [en.md](en.md) |
| 🇧🇷 Português (Brasil) | [pt-BR.md](pt-BR.md) |
| 🇪🇸 Español | [es.md](es.md) |

> The three files are kept in sync. When you change one, update the other two in the same
> pull request.

## What you will build

A full cloud-native Java platform: a secured Spring Boot API, an Angular SPA, Keycloak for
authentication, PostgreSQL (with pgvector) for data and semantic search, MinIO for images,
an embeddings sidecar, optional OpenAI integration, observability, and a CI/CD pipeline that
deploys to Kubernetes (k3s).

See the diagrams and step-by-step instructions in your language file above.
