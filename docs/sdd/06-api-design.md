# API Design

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

API requests are authenticated with bearer tokens issued by Keycloak. Authorization rules should be documented per endpoint family as role-based behavior matures.
