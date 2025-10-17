# Project Structure

Spring Boot 3.5 (Java 21) backend with PostgreSQL, Flyway, stateless JWT auth, and SSE streaming of AI responses via OpenAI.

```
ai-prompt-backend/
├─ pom.xml                       # Maven config (Java 21, dependencies)
├─ mvnw / mvnw.cmd               # Maven wrapper scripts
├─ Dockerfile                    # Container build (service on /api)
├─ ARCHITECTURE.md               # High-level architecture overview
├─ HELP.md                       # Spring Boot starter help
├─ README.md                     # Setup, configuration, API examples
└─ src/
   ├─ main/java/com/sallyvnge/aipromptbackend/
   │  ├─ AiPromptBackendApplication.java     # Spring Boot entrypoint
   │  ├─ api/
   │  │  ├─ controller/                     # REST & SSE controllers
   │  │  │  ├─ AuthController.java          # /auth: login, refresh
   │  │  │  ├─ UserController.java          # /users/me: current profile
   │  │  │  ├─ ThreadController.java        # /threads: create/list/get/patch
   │  │  │  ├─ MessageController.java       # /threads/{id}/messages: list/create
   │  │  │  └─ AiSseController.java         # /threads/{id}/respond (SSE)
   │  │  ├─ dto/                            # Request/response DTOs
   │  │  │  ├─ auth/ (LoginRequest, RefreshRequest, TokenResponse)
   │  │  │  ├─ thread/ (CreateThreadDto, PatchThreadDto, ThreadDto, PageDto)
   │  │  │  ├─ message/ (CreateMessageDto, MessageDto, RespondRequest)
   │  │  │  └─ users/ (UserMeDto)
   │  │  └─ exception/                      # Consistent error responses
   │  │     └─ GlobalExceptionHandler.java
   │  ├─ config/                            # App/runtime configuration
   │  │  ├─ OpenAiConfig.java               # OpenAI client wiring
   │  │  ├─ CurrentUser.java                # Current user record
   │  │  ├─ CurrentUserProvider.java        # Interface to access/set current user
   │  │  ├─ RequestScopedCurrentUserProvider.java
   │  │  ├─ CurrentUserFilter.java          # Populates CurrentUser per request
   │  │  └─ DevHeaderUserResolver.java      # Dev-only (X-Demo-Email)
   │  ├─ domain/                            # JPA entities & domain ports
   │  │  ├─ UserEntity.java
   │  │  ├─ ThreadEntity.java
   │  │  ├─ MessageEntity.java
   │  │  └─ port/AiProvider.java            # Abstraction for AI streaming
   │  ├─ infrastructure/                    # External adapters/implementations
   │  │  ├─ OpenAiProvider.java             # OpenAI-backed streaming (@Primary)
   │  │  └─ MockAiProvider.java             # Local/dev mock streaming
   │  ├─ repository/                        # Spring Data JPA repositories
   │  │  ├─ UserRepository.java
   │  │  ├─ ThreadRepository.java
   │  │  └─ MessageRepository.java          # appendDelta/markError writes
   │  ├─ security/                          # Security & JWT
   │  │  ├─ SecurityConfig.java             # Stateless, CORS, filter chain
   │  │  ├─ JwtAuthFilter.java              # Bearer token processing
   │  │  ├─ JwtService.java                 # Issue/parse JWTs
   │  │  └─ JwtServiceImpl.java
   │  ├─ service/                           # Application services
   │  │  ├─ AppUserDetailsService.java      # Loads users for auth
   │  │  ├─ ThreadService.java              # Thread lifecycle
   │  │  └─ MessageService.java             # Message lifecycle (SSE assembly)
   │  └─ BCryptOnce.java                    # One-off BCrypt hash utility
   ├─ main/resources/
   │  ├─ application.yml                    # Properties (context-path=/api)
   │  ├─ .env.local                         # Example local env (do not commit secrets)
   │  └─ db/migration/                      # Flyway SQL migrations
   │     ├─ V1__init.sql
   │     ├─ V2__assistant_streaming.sql
   │     ├─ V3__auth_password.sql
   │     └─ V4__add_conversational_memory.sql
   └─ test/java/com/sallyvnge/aipromptbackend/
      └─ AiPromptBackendApplicationTests.java # Context load test
```

Key runtime notes
- Base path: all endpoints are served under `/api` (see `server.servlet.context-path`).
- CORS: configured via `security.cors.allowed-origins` (comma-separated).
- Migrations: Flyway auto-runs `src/main/resources/db/migration` on startup.
- Auth: stateless JWT (access + refresh); public routes under `/auth/**` and `/actuator/**`; everything else requires a valid access token.
- Current user: resolved via `JwtAuthFilter` principal; in dev, `DevHeaderUserResolver` reads `X-Demo-Email`. `GET /users/me` returns `{ email, displayName, avatarUrl }`.
- SSE: streaming responses are handled by `AiSseController` and an `AiProvider` implementation. Events: `token` (delta), `done`, `error`.

Data flow (happy path)
1) Client authenticates (`POST /auth/login`) and receives access/refresh tokens.
2) Client creates a thread (`POST /threads`).
3) Client posts a user message (`POST /threads/{id}/messages`).
4) Client requests an assistant response stream (`POST /threads/{id}/respond`) and consumes SSE `token` events until `done`.
5) Server persists deltas and final content; thread `updatedAt` advances for pagination.

Persistence notes
- Messages: assistant deltas are appended via `MessageRepository.appendDelta`, then committed to `content` on finalize; errors marked via `markError`.
- Threads: pagination is time-based on `updatedAt` via `ThreadRepository.pageByUser`.
- Migrations: V4 adds initial structures for conversational memory (`user_memory`, `thread_summary`, `memory_episode`, `memory_chunk`).

Configuration (see README for details)
- Database: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- OpenAI: `openai.api-key` (required), optional `openai.base-url`, `openai.org-id`, `openai.project-id`, `openai.timeout-seconds`.
- JWT: `jwt.secret`, `jwt.exp-minutes` (default 15), `jwt.refresh-days` (default 14).
- CORS: `security.cors.allowed-origins`.

---

For setup, usage, and API examples, see `README.md`.

