# Project Structure

This backend is a Spring Boot 3.5 (Java 21) service with PostgreSQL, Flyway migrations, JWT-based auth, and SSE streaming for AI responses via OpenAI.

```
ai-prompt-backend/
├─ pom.xml                         # Maven config (Java 21, dependencies)
├─ mvnw / mvnw.cmd                 # Maven wrapper scripts
├─ ARCHITECTURE.md                 # High-level architecture overview
├─ HELP.md                         # Spring Boot starter help
└─ src/
   ├─ main/java/com/sallyvnge/aipromptbackend/
   │  ├─ AiPromptBackendApplication.java    # Spring Boot entrypoint
   │  ├─ api/
   │  │  ├─ controller/                    # REST & SSE controllers
   │  │  │  ├─ AuthController.java         # /auth: login, refresh
   │  │  │  ├─ ThreadController.java       # /threads: create/list/get/patch
   │  │  │  ├─ MessageController.java      # /threads/{id}/messages: list/create
   │  │  │  └─ AiSseController.java        # /threads/{id}/respond (SSE)
   │  │  ├─ dto/                           # Request/response DTOs
   │  │  │  ├─ auth/ (LoginRequest, TokenResponse, RefreshRequest)
   │  │  │  ├─ thread/ (CreateThreadDto, PatchThreadDto, ThreadDto, PageDto)
   │  │  │  └─ message/ (CreateMessageDto, MessageDto, RespondRequest)
   │  │  └─ exception/
   │  │     └─ GlobalExceptionHandler.java # Consistent error responses
   │  ├─ config/                           # App/runtime configuration
   │  │  ├─ OpenAiConfig.java              # OpenAI client wiring
   │  │  ├─ CurrentUser* (record/provider/filter)
   │  │  └─ DevHeaderUserResolver.java     # Dev-only user resolution (X-Demo-Email)
   │  ├─ domain/                           # JPA entities & domain ports
   │  │  ├─ UserEntity.java
   │  │  ├─ ThreadEntity.java
   │  │  ├─ MessageEntity.java
   │  │  └─ port/AiProvider.java           # Abstraction for AI streaming
   │  ├─ infrastructure/                   # External adapters/implementations
   │  │  ├─ OpenAiProvider.java            # OpenAI-backed streaming
   │  │  └─ MockAiProvider.java            # Local/dev mock streaming
   │  ├─ repository/                       # Spring Data JPA repositories
   │  │  ├─ UserRepository.java
   │  │  ├─ ThreadRepository.java
   │  │  └─ MessageRepository.java         # delta/markError writes
   │  ├─ security/                         # Security & JWT
   │  │  ├─ SecurityConfig.java            # Stateless, CORS, filter chain
   │  │  ├─ JwtAuthFilter.java             # Bearer token processing
   │  │  ├─ JwtService(.java/.Impl.java)   # Issue/parse JWTs
   │  └─ service/                          # Application services
   │     ├─ AppUserDetailsService.java     # Loads users for auth
   │     ├─ ThreadService.java             # Thread lifecycle
   │     └─ MessageService.java            # Message lifecycle (SSE assembly)
   └─ main/resources/
      ├─ application.yml                   # Properties (context-path=/api)
      ├─ .env.local                        # Example local DB vars (do not commit secrets)
      └─ db/migration/                     # Flyway SQL migrations (V1..V3)
```

Key runtime notes
- Base path: all endpoints are served under `/api` (see `server.servlet.context-path`).
- CORS: configured via `security.cors.allowed-origins` (comma-separated).
- Migrations: Flyway auto-runs `src/main/resources/db/migration` on startup.
- Auth: stateless JWT (access + refresh); public routes under `/auth/**` and `/actuator/**`.
- SSE: streaming responses come from `AiSseController` and `AiProvider` (OpenAI or mock).

Data flow (happy path)
1) Client authenticates (`/auth/login`) and receives access/refresh tokens.
2) Client creates a thread (`POST /threads`).
3) Client posts a user message (`POST /threads/{id}/messages`).
4) Client requests an assistant response stream (`POST /threads/{id}/respond`) and consumes SSE `token` events until `done`.
5) Server persists deltas and final content; thread `updatedAt` advances for pagination.

Environment variables (see README for details)
- Database: `DB_URL`, `DB_USER`, `DB_PASSWORD`.
- OpenAI: `openai.api-key`, optional `openai.base-url`, `openai.org-id`, `openai.project-id`, `openai.timeout-seconds`.
- JWT: `jwt.secret`, optional `jwt.exp-minutes`, `jwt.refresh-days`.
- CORS: `security.cors.allowed-origins`.

---

For setup, usage, and API examples, see README.md.
