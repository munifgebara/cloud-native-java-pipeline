# API Design

> Part of the [Software Design Document](README.md). The live, browsable contract is the
> Scalar UI at `/scalar` (OpenAPI at `/v3/api-docs`).

## Resource Map

```mermaid
flowchart LR
    subgraph public[/api/public · no auth/]
        L[POST /login]
        IMG[GET images: main-items · locations · people]
    end
    subgraph v0[/api/v0 · JWT required/]
        DASH[dashboard/summary]
        CAT[categories]
        LOC[locations]
        MI[main-items<br/>+ semantic-search · image-ai]
        INST[instances-item]
        MOV[movements-item<br/>inbound · outbound · transfer]
        LOAN[loans-item<br/>+ return]
        PPL[people]
        AI[ai/registration-photo]
        USR[users · admin only]
    end
    Client[SPA / API client] --> public
    Client --> v0
```

## REST Conventions

Stella APIs are versioned under `/api/v0` today. New APIs should preserve predictable resource naming, explicit request/response DTOs and consistent error responses.

## Endpoint Patterns

| Pattern | Use |
| --- | --- |
| `GET /resources` | List resources with filters or pagination when needed. |
| `GET /resources/{id}` | Fetch a single resource. |
| `POST /resources` | Create a resource or start a command-style operation. |
| `PUT /resources/{id}` | Replace or update a known resource when full state is supplied. |
| `PATCH /resources/{id}` | Partial updates when supported by the domain. |
| `DELETE /resources/{id}` | Logical or administrative delete, according to domain rules. |

## Contracts

- Use DTOs at API boundaries.
- Avoid leaking JPA entities through controllers.
- Validate request payloads before business operations.
- Keep file upload contracts explicit about accepted content type and size.

## Versioning

The current version namespace is `/api/v0`. Breaking contract changes should either remain internal before public use or introduce a new versioned API path.

## Error Responses

Errors should be structured and stable enough for the frontend to display useful feedback. Expected errors should not be returned as generic 500 responses.

## Authentication

`/api/v0/**` requests are authenticated with a Bearer JWT. Tokens are obtained through the
backend-mediated login (`POST /api/public/login`), not directly from Keycloak — see
[Security](07-security.md). Administrative endpoints under `/api/v0/users` additionally require
the `admin` role.
