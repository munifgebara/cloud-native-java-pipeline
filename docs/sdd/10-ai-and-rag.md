# AI and RAG

## AI Architecture

AI capabilities should be accessed through backend providers and service abstractions. Controllers should not depend directly on external model SDKs or provider-specific payloads.

## LLM Integration

OpenAI-backed features currently support image analysis and generated image workflows. External calls should have explicit timeout, error handling, cost controls and user-facing fallback behavior.

## Embeddings

Embedding design should document:

- selected provider or local model
- vector dimensions
- source text used to produce embeddings
- update rules when source data changes
- batch and retry strategy
- storage location

## Vectorization

Vector data must remain synchronized with inventory data. Item inserts, updates and deletes should update or remove corresponding vector entries as part of the semantic search design.

## RAG

RAG design should define:

- retrievable document types
- chunking strategy
- ranking strategy
- prompt assembly
- source attribution
- fallback behavior when retrieval quality is low

## Agents

Agent workflows should be introduced with clear boundaries:

- what the agent may read
- what the agent may change
- which operations need human approval
- how actions are logged
- how tool failures are surfaced

## Prompt Engineering

Prompts should be versioned in code or documentation when they affect product behavior. Prompts must avoid embedding secrets or hidden operational credentials.

## Evaluation Strategy

Future AI evaluation should include representative examples, failure cases, safety checks and regression tests where deterministic assertions are practical.
