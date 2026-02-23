# Notify Bridge

Multi-channel notification service with a Telegram bot UI and REST API. Routes alerts through **Telegram** and **Pushover** (critical alerts with iOS DND bypass).

Built with Kotlin 2.1, Spring Boot 3.4, and PostgreSQL.

## Architecture

```
                     ┌──────────────────────┐
                     │    REST API (OAuth2)  │
                     │  POST /notifications  │
                     └──────────┬───────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
        ▼                       ▼                       ▼
 ┌─────────────┐      ┌────────────────┐      ┌────────────────┐
 │  Telegram    │      │   Pushover     │      │  Rate Limiter  │
 │  Bot API     │      │   API          │      │  (Token Bucket)│
 └──────┬──────┘      └───────┬────────┘      └────────────────┘
        │                     │
        ▼                     ▼
 ┌─────────────┐      ┌────────────────┐
 │  Bot UI     │      │  iOS/Android   │
 │  (commands, │      │  Push with     │
 │  keyboards) │      │  DND bypass    │
 └─────────────┘      └────────────────┘
        │
        ▼
 ┌─────────────────────────────────────┐
 │  PostgreSQL + Flyway Migrations     │
 │  (users, invitations, pushover cfg) │
 └─────────────────────────────────────┘
```

**Layered architecture:**

```
Handler (Telegram UI)  →  Service (Business Logic)  →  Facade (@Transactional)  →  Repository (JPA)
Controller (REST API)  →  Service                   →  Facade                   →  Repository
```

## Features

- **Telegram Bot** — inline keyboards, multi-step dialogs, role-based commands
- **Pushover Integration** — priorities from silent (-2) to emergency (+2), custom sounds, DND bypass
- **REST API** — OAuth2/JWT-protected endpoints for programmatic notifications
- **Rate Limiting** — global + per-chat Token Bucket rate limiter for Telegram API
- **Invitation System** — user access controlled through admin invitations
- **Limits Monitoring** — automatic alerts when Pushover usage reaches thresholds (50%, 25%, 10%, 5%)
- **Health Checks** — integrated healthchecks.io monitoring with DB and API checks
- **Client Library** — `notifier-api` module with Spring Boot AutoConfiguration for service-to-service calls

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1, Java 21 |
| Framework | Spring Boot 3.4 |
| Security | Spring Security, OAuth2 Resource Server (Auth0) |
| Database | PostgreSQL 17, Spring Data JPA, Flyway |
| Bot | [telegram-bot](https://github.com/DEHuckaback/telegram-bot) library |
| Notifications | Telegram Bot API, Pushover API |
| Build | Gradle (Kotlin DSL), axion-release (SemVer from Git tags) |
| CI/CD | GitHub Actions, Docker (multi-stage), ArgoCD |
| Testing | JUnit 5, Testcontainers, Mockito-Kotlin |

## Quick Start

```bash
# Start PostgreSQL
docker-compose up -d

# Configure environment
cp .env.example .env
# Edit .env with your Telegram bot token and other settings

# Run
./gradlew bootRun

# Format code
./gradlew ktlintFormat
```

### Required Environment Variables

| Variable | Description |
|----------|-------------|
| `TELEGRAM_BOT_TOKEN` | Telegram bot token from [@BotFather](https://t.me/BotFather) |
| `TELEGRAM_BOT_USERNAME` | Bot username (without @) |
| `MASTER_ADMIN_ID` | Telegram user ID of the master admin |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | PostgreSQL connection |
| `DB_USERNAME`, `DB_PASSWORD` | Database credentials |

### Optional Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PUSHOVER_ENABLED` | `false` | Enable Pushover integration |
| `PUSHOVER_API_TOKEN` | — | Pushover API token |
| `AUTH0_ENABLED` | `true` | Enable OAuth2 for REST API |
| `AUTH0_ISSUER` | — | Auth0 issuer URL |
| `AUTH0_AUDIENCE` | — | Auth0 API audience |
| `LOG_FORMAT` | `none` | `ecs` for JSON (Loki), `none` for plain text |

## REST API

All endpoints require OAuth2 JWT authentication (when `AUTH0_ENABLED=true`).

| Method | Path | Scope | Description |
|--------|------|-------|-------------|
| `POST` | `/api/v1/notifications/telegram` | `send:telegram` | Send Telegram message |
| `POST` | `/api/v1/notifications/pushover` | `send:pushover` | Send Pushover notification |
| `GET` | `/api/v1/notifications/limits` | `read:limits` | Get usage limits |
| `GET` | `/api/ping` | authenticated | Health check |
| `GET` | `/actuator/health` | public | Spring Actuator health |

### Example: Send Telegram Notification

```bash
curl -X POST http://localhost:8090/api/v1/notifications/telegram \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "Server disk usage above 90%", "parseMode": "HTML"}'
```

## Telegram Bot Commands

| Command | Role | Description |
|---------|------|-------------|
| `/start` | Public | Register (requires invitation) |
| `/help` | User | List available commands |
| `/settings` | User | Configure Pushover, timezone |
| `/admin` | Admin | Admin panel |
| `/list_users` | Admin | List registered users |
| `/add_user` | Admin | Create invitation for new user |
| `/remove_user` | Admin | Remove user |

## Project Structure

```
notifier/
├── src/main/kotlin/.../notifier/
│   ├── bot/            # Bot commands, callbacks, steps, security DSL
│   ├── config/         # Spring config, properties, security
│   ├── controller/     # REST API endpoints
│   ├── handler/        # Telegram bot event handlers
│   ├── model/          # Domain models, sealed classes
│   ├── persistence/    # JPA entities, repositories, facades
│   ├── pushover/       # Pushover API client and service
│   ├── service/        # Business logic
│   ├── telegram/       # Rate limiting, message service
│   └── util/           # Utilities
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/   # Flyway migrations (V1-V6)
├── notifier-api/       # Client library module (published to Maven)
├── .github/workflows/  # CI, Release, Release-API
├── Dockerfile          # Multi-stage build (Alpine JRE, non-root)
└── docker-compose.yml  # Local development (PostgreSQL)
```

## Client Library (notifier-api)

The `notifier-api` module is a Spring Boot starter that other services can use to send notifications programmatically.

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.smhomelab:notifier-api:0.1.0")
}
```

```yaml
# application.yml
smhomelab:
  notifier:
    base-url: http://notifier:8090
    oauth2:
      token-uri: https://your-tenant.auth0.com/oauth/token
      client-id: ${NOTIFIER_CLIENT_ID}
      client-secret: ${NOTIFIER_CLIENT_SECRET}
      audience: https://api.your-app.com
```

```kotlin
@Service
class AlertService(private val notifierClient: NotifierClient) {

    suspend fun sendAlert(message: String) {
        notifierClient.sendTelegram(
            TelegramNotificationRequest(message = message, parseMode = "HTML")
        )
    }
}
```

## Testing

```bash
# Unit tests
./gradlew test

# Integration tests (requires Docker)
./gradlew integrationTest

# All tests
./gradlew test integrationTest
```

## License

MIT
