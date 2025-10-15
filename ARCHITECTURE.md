# AI Prompt Backend — Architecture

This document describes how the backend is structured today, what it does, and how its pieces fit together. It reflects the current codebase and configuration.

## Overview

- Spring Boot 3.5.6 (Java 21), Maven build
- PostgreSQL with Flyway migrations
- REST API served under the base path `/api`
- Server-Sent Events (SSE) for streaming assistant responses
- Layered design: API → Service → Domain/Ports → Repository → Infrastructure

## Tech Stack

- Spring Boot Web, Validation, Data JPA, Actuator
- Spring Security (foundation in place; permissive by default)
- Lombok for boilerplate reduction
- PostgreSQL driver + Flyway
- OpenAI Java SDK for streaming completions

## Layered Architecture

```
API (Controllers & DTOs)
  - ThreadController
  - MessageController
  - AiSseController (SSE endpoint)

Service (Business logic)
  - ThreadService
  - MessageService

Domain (Entities & Ports)
  - UserEntity, ThreadEntity, MessageEntity
  - port/AiProvider (abstraction for AI vendors)

Repository (Persistence)
  - UserRepository, ThreadRepository, MessageRepository

Infrastructure (Adapters)
  - OpenAiProvider (@Primary)
  - MockAiProvider (dev/testing)
```

### Request Flow (SSE respond)

1) Client calls `POST /api/threads/{threadId}/respond` with a `RespondRequest` (prompt, optional model, optional systemPrompt).
2) `MessageService.createAssistantDraft` creates a streaming assistant message (status `streaming`) and bumps the thread `updatedAt`.
3) A background thread calls `AiProvider.respondStream(...)`:
   - On delta: `MessageRepository.appendDelta` appends to `content_delta` and an SSE `token` event is emitted.
   - On done: `MessageService.finalizeAssistantMessage` moves `content_delta` → `content`, sets status to `complete`, and emits SSE `done` (`[DONE]`).
   - On error: `MessageRepository.markError` stores error details and an SSE `error` event is emitted.

Default model behavior today:
- New threads: `ThreadService.create` defaults to `o4-mini` if no model is supplied.
- SSE provider fallback: `OpenAiProvider` falls back to `gpt-4o-mini` if model is blank.

## Data Model (DDL via Flyway)

Tables and constraints are defined under `src/main/resources/db/migration`:

- users
  - id (uuid, pk), email (unique), display_name
  - password_hash, status (ACTIVE|LOCKED|DISABLED), failed_attempts, last_login_at
  - created_at, updated_at

- threads
  - id, user_id (fk users)
  - title, model, status (default `active`), system_prompt, summary
  - metadata (jsonb), version (optimistic lock), created_at, updated_at
  - index: (user_id, updated_at desc)

- messages
  - id, thread_id (fk threads)
  - author (user|assistant|system), position
  - status (draft|streaming|complete|error)
  - content, content_delta
  - model, usage_prompt_tokens, usage_completion_tokens, latency_ms
  - error_code, error_message
  - created_at, updated_at
  - index: (thread_id, position)

Note: not all columns are surfaced by the DTOs yet (e.g., `error_*`, `metadata`).

## API Surface

Base path: `/api`

Threads
- POST `/threads` → create a thread
- GET `/threads?limit=&cursor=` → list threads for current user (cursor is `updatedAt` ISO-8601; max 50)
- GET `/threads/{id}` → get a thread (owner-only)
- PATCH `/threads/{id}` → patch title/status/model/systemPrompt with optimistic locking (`version`)

Messages
- GET `/threads/{threadId}/messages?afterPosition=&limit=` → list messages (ascending position; default limit 50)
- POST `/threads/{threadId}/messages` → create a user message (`author='user'`)
- POST `/threads/{threadId}/respond` → start assistant streaming via SSE

### SSE Contract (respond)

- Content-Type: `text/event-stream`
- Events emitted:
  - `token`: incremental content delta
  - `done`: `[DONE]` when the provider completes
  - `error`: error message if the provider fails

## DTOs (selected)

- ThreadDto: `id, title, model, status, systemPrompt, summary, createdAt, updatedAt, version`
- MessageDto: `id, threadId, author, position, status, content, contentDelta, model, usagePromptTokens, usageCompletionTokens, latencyMs, createdAt`
- PageDto<T>: `items, nextCursor`

## Security and Current User

- Requests are permitted by default (no enforced login yet).
- `CurrentUserFilter` derives the current user from the `X-Demo-Email` header; if absent, falls back to `me@example.com`.
- `ThreadService` ensures thread ownership per request (`GET`/`PATCH`).
- JWT scaffolding exists but is not wired yet; production hardening will replace the dev header with authenticated principals.

## Configuration

Application configuration lives in `src/main/resources/application.yml` and environment variables. Key properties:

- Database: `DB_URL`, `DB_USER`, `DB_PASSWORD`
- HTTP: `server.servlet.context-path=/api`
- CORS: `security.cors.allowed-origins` (comma-separated allowed origins)
- OpenAI: `OPENAI_API_KEY`, `OPENAI_BASE_URL`, `OPENAI_ORG_ID`, `OPENAI_PROJECT_ID`, optional model override `OPENAI_MODEL`
- JWT (planned): `JWT_SECRET`, expiration minutes
- Actuator: exposes `health,info,metrics,prometheus`

## OpenAI Integration

- `OpenAiProvider` is the primary `AiProvider` implementation and streams chat completions using the OpenAI Java SDK.
- Provider respects optional `systemPrompt` and user `prompt` and emits deltas as they arrive.
- `MockAiProvider` offers a deterministic, no-network streaming experience for local testing.

## Notes & Future Work

- Unify default model naming (`o4-mini` vs `gpt-4o-mini`) across thread creation and provider fallback.
- Map additional fields to DTOs when needed (e.g., `error_code`, `error_message`, thread `metadata`).
- Replace dev header user resolution with actual authentication (e.g., JWT) and wire SecurityFilterChain accordingly.

