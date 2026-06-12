# Introduction

## Purpose

This document introduces the Stella Software Design Document and defines the scope of the system design material kept under `docs/sdd/`.

## Stella Overview

Stella is a cloud-native personal inventory management system. It combines a Spring Boot API, an Angular frontend, Keycloak authentication, PostgreSQL persistence, MinIO image storage, Kubernetes deployment assets and CI/CD automation.

The system is evolving toward richer inventory workflows, observability, AI-assisted operations, semantic search, RAG and agent-based features.

## Scope

The SDD covers:

- product and system context
- high-level architecture
- backend, frontend and infrastructure components
- data model and persistence strategy
- API design conventions
- security model
- deployment model
- observability approach
- AI, embeddings, vector search, RAG and agent design
- architecture decision records

## Strategic Goals

- Keep architecture decisions visible and versioned with the code.
- Support onboarding for developers and operators.
- Separate design intent from operational how-to guides.
- Provide a place for incremental design work before and after implementation.

## Relationship With Existing Documentation

The root `README.md` gives a project overview and local entry points. The guides in `docs/` describe development, configuration, testing, deployment and operations. This SDD records the software design behind those guides and should link back to them when detailed procedures already exist elsewhere.
