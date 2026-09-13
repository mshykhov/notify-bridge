# Notify Bridge

A Kotlin and Spring Boot notification service with a Telegram bot, Pushover integration, and an OAuth2-protected REST API. PostgreSQL stores users, invitations, and notification settings.

## Run locally

Requires Java 21, Docker with Compose, a Telegram bot from [BotFather](https://t.me/BotFather), and an Auth0 API for REST authentication.

```bash
git clone https://github.com/mshykhov/notify-bridge.git
cd notify-bridge
cp .env.example .env
```

Set `TELEGRAM_BOT_TOKEN`, `TELEGRAM_BOT_USERNAME`, and your numeric `MASTER_ADMIN_ID`. Set `AUTH0_ISSUER` and `AUTH0_AUDIENCE` for your Auth0 API. The example database settings match the local Compose service.

```bash
docker compose up -d postgres
./gradlew bootRun --args='--spring.config.import=file:.env[.properties]'
```

The explicit [Spring config import](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html#features.external-config.files.importing.extensionless) loads `.env` as Java properties; keep values unquoted. `bootRun` alone does not load that file. With the example settings, the server listens on port 8090.

Open your bot and use `/start`. The master admin can invite users through `/admin`; users configure Pushover and timezone through `/settings`. Pushover is optional: enable `PUSHOVER_ENABLED` and supply `PUSHOVER_API_TOKEN` to use it.

## REST API

Keep `AUTH0_ENABLED=true` and request a JWT with the scopes needed by your client. Disabling the resource-server configuration does not remove method-level scope checks.

| Method | Path | Required scope |
|---|---|---|
| POST | `/api/v1/notifications/telegram` | `send:telegram` |
| POST | `/api/v1/notifications/pushover` | `send:pushover` |
| GET | `/api/v1/notifications/limits` | `read:limits` |
| GET | `/api/ping` | Authenticated |
| GET | `/actuator/health` | Public |

This sends a real message to `MASTER_ADMIN_ID`:

```bash
curl http://localhost:8090/api/v1/notifications/telegram \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message":"Example notification","parseMode":"HTML"}'
```

## Client library

`notifier-api` contains a Spring Boot client and DTOs. Its publishing configuration targets Maven Local:

```bash
./gradlew :notifier-api:publishToMavenLocal
./gradlew :notifier-api:currentVersion
```

Add `mavenLocal()` to the consuming project's repositories, then depend on `com.smhomelab:notifier-api:<reported-version>`. Configure `smhomelab.notifier.base-url` and its `oauth2` properties (`token-uri`, `client-id`, `client-secret`, `audience`). No Maven Central publication is assumed.

## Structure and checks

- `src/main/kotlin/`: bot handlers, REST controllers, services, rate limiting, and persistence.
- `src/main/resources/db/migration/`: Flyway schema migrations.
- `notifier-api/`: reusable HTTP client.
- `docker-compose.yml`: local PostgreSQL.

```bash
./gradlew test                   # Unit tests
./gradlew integrationTest        # Testcontainers; requires Docker
./gradlew ktlintFormat           # Kotlin formatting
```

[MIT License](LICENSE)
