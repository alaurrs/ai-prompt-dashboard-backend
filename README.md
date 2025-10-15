# AI Prompt Backend

Spring Boot 3.5 (Java 21) backend for managing chat threads/messages and streaming AI responses (SSE) via OpenAI. Uses PostgreSQL + Flyway, stateless JWT auth, and a clean layering (controller → service → repository/domain → infrastructure).

- Stack: Spring Boot, JPA/Hibernate, Flyway, Spring Security (JWT), OpenAI Java (4.3.0)
- Java: 21
- Build: Maven (wrapper included)
- Base path: `/api` (see `server.servlet.context-path`)

## Quickstart

Prerequisites
- Java 21
- Maven 3.9+ (or use the Maven Wrapper `mvnw/mvnw.cmd`)
- PostgreSQL 14+ (local or hosted)

Configure environment
- Provide DB credentials and API keys via env vars or your IDE run config.
- Example variables (do not commit secrets):

```
DB_URL=jdbc:postgresql://localhost:5432/aiprompt
DB_USER=postgres
DB_PASSWORD=postgres

# OpenAI (required for real streaming; otherwise MockAiProvider can be used)
openai.api-key=sk-...            
openai.base-url=https://api.openai.com/v1   # optional
openai.org-id=...                          # optional
openai.project-id=...                      # optional
openai.timeout-seconds=0                   # 0 means no per-request override

# JWT
jwt.secret=change-me-32+chars
jwt.exp-minutes=15
jwt.refresh-days=14

# CORS
security.cors.allowed-origins=http://localhost:4200
```

Run (dev)
- Windows: `./mvnw.cmd spring-boot:run`
- Linux/macOS: `./mvnw spring-boot:run`

On startup
- Flyway runs SQL migrations under `src/main/resources/db/migration`.
- Service listens on `http://localhost:8080/api` by default.

Optional: Dockerized Postgres
```
docker run --name aiprompt-pg -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=aiprompt -p 5432:5432 -d postgres:16
```

## Configuration Reference

Database
- `DB_URL` – JDBC URL (e.g., `jdbc:postgresql://localhost:5432/aiprompt`)
- `DB_USER`, `DB_PASSWORD`

OpenAI
- `openai.api-key` – API key for real completions
- `openai.base-url` – override for compatible endpoints (optional)
- `openai.org-id`, `openai.project-id` – optional scoping
- `openai.timeout-seconds` – per-client timeout; `0` uses defaults

JWT
- `jwt.secret` – HMAC key (>= 32 chars recommended)
- `jwt.exp-minutes` – access token TTL (default 15)
- `jwt.refresh-days` – refresh token TTL (default 14)

CORS & Server
- `security.cors.allowed-origins` – comma-separated list
- `server.servlet.context-path` – defaults to `/api`

Notes
- File `src/main/resources/.env.local` is an example for local development only. Replace with your own credentials and avoid committing secrets.

## API Overview

Base URL
- `http://localhost:8080/api`
- All non-auth routes require `Authorization: Bearer <accessToken>`.
- For dev user association, header `X-Demo-Email` is read by `DevHeaderUserResolver`; defaults to `me@example.com` if absent.

Auth
- `POST /auth/login`
  - Body: `{ "email": "user@example.com", "password": "..." }`
  - Response: `{ accessToken, refreshToken, tokenType, expiresIn }`
  - Example:
    ```bash
    curl -s http://localhost:8080/api/auth/login \
      -H 'Content-Type: application/json' \
      -d '{"email":"user@example.com","password":"pass"}'
    ```
- `POST /auth/refresh`
  - Body: `{ "refreshToken": "..." }`
  - Response: same shape as login

Threads
- `POST /threads`
  - Body:
    ```json
    { "title": "New chat", "model": "o4-mini", "systemPrompt": "You are helpful." }
    ```
  - Response: `ThreadDto`
- `GET /threads?limit=20&cursor=<ISO-8601>`
  - Response: `PageDto<ThreadDto>` with `nextCursor`
- `GET /threads/{id}` → `ThreadDto`
- `PATCH /threads/{id}`
  - Body must include current `version` for optimistic locking.
  - Example:
    ```json
    { "title": "Renamed", "version": 0 }
    ```

Messages
- `GET /threads/{threadId}/messages?afterPosition=-1&limit=50`
  - Returns list of `MessageDto`, ascending by position
- `POST /threads/{threadId}/messages`
  - Body:
    ```json
    { "author": "user", "content": "Hello!" }
    ```
  - Response: `MessageDto`

Assistant Streaming (SSE)
- `POST /threads/{threadId}/respond` with `Accept: text/event-stream`
  - Body (all fields optional; `model` falls back to thread):
    ```json
    { "prompt": "Explain X", "model": "gpt-4o-mini", "systemPrompt": "You are helpful." }
    ```
  - Events:
    - `event: token` for incremental deltas
    - `event: done` at completion
    - `event: error` on failure
  - Example (curl):
    ```bash
    curl -N http://localhost:8080/api/threads/<threadId>/respond \
      -H 'Accept: text/event-stream' \
      -H 'Authorization: Bearer <accessToken>' \
      -H 'X-Demo-Email: user@example.com' \
      -H 'Content-Type: application/json' \
      -d '{"prompt":"Hi there"}'
    ```

## Development Notes

- Security: JWT is enforced for all non-`/auth/**` and non-`/actuator/**` routes. Refresh tokens are rejected on protected routes.
- Current User: For now, the `CurrentUser` is resolved from `X-Demo-Email` (dev convenience). For production, wire it to the authenticated principal.
- Pagination: Threads list uses time-based cursors (`updatedAt`), `nextCursor` is an ISO-8601 string.
- SSE Storage: Assistant content is accumulated in `contentDelta` then committed to `content` on completion.

## Project Layout

See `PROJECT_STRUCTURE.md` for a directory map and responsibilities.
