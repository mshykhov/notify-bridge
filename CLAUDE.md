# Notify Bridge

**TL;DR:** Multi-channel notification service with Telegram bot UI and REST API. Routes alerts through Telegram and Pushover (critical alerts with DND bypass). PostgreSQL for persistence.

> **Stack**: Kotlin 2.1, Spring Boot 3.4, Java 21, PostgreSQL 17

---

## Quick Start

```bash
docker-compose up -d          # PostgreSQL
cp .env.example .env          # Configure (fill in)
./gradlew bootRun             # Run
./gradlew ktlintFormat        # Format code
./gradlew test                # Unit tests
./gradlew integrationTest     # Integration tests (requires Docker)
```

- DB DSN: `notifier:notifier@localhost:5432/notifier`
- Required ENV: see `.env.example`
- See README for full documentation

---

## Architecture

```
Handler → Service → Facade → Repository
   ↓         ↓         ↓          ↓
 Telegram  Business  @Trans    Spring
 events    logic     action    Data JPA
```

| Layer | Responsibility | Characteristics |
|-------|---------------|-----------------|
| **Handler** | Bot events, UI flow, inline keyboards | suspend, knows only Service |
| **Service** | Business logic, validation, integrations | suspend, coroutines |
| **Facade** | Transactional wrappers | @Transactional, synchronous |
| **Repository** | DB access | Spring Data JPA |

---

## Development Principles

### Kotlin/Spring Style
- **Immutability**: prefer `val`, data classes
- **Sealed classes**: for results (ValidationResult, SendResult)
- **Suspend functions**: in Handler and Service layers
- **Extension functions**: for domain-specific DSL

### Security
- **All commands require role check** via `secureCommand()`, `secureCallback()`, `secureStep()`
- Master Admin from `MASTER_ADMIN_ID` — cannot be removed
- New users only through invitation flow

```kotlin
// Correct
secureCommand(BotCommands.SETTINGS) { ... }

// Wrong — no role check
command("/settings") { ... }
```

### Database
- **Flyway**: migrations in `db/migration/V{N}__*.sql`
- **Hibernate**: `ddl-auto: validate` — only Flyway changes schema
- New migration = new file `V{N+1}__description.sql`

### Integrations
- **Telegram**: polling mode, inline keyboards, multi-step flows
- **Pushover**: priorities LOWEST(-2) to EMERGENCY(2), limits monitoring

---

## Code Policy

### Architecture
- **Layered structure**: Handler → Service → Facade → Repository
- Clear separation of concerns between layers

### Principles
- **DRY**: Don't repeat logic, extract reusable components
- **SOLID**: All five principles
- **KISS**: Simple solutions over complex ones

### Organization
- **One file = one class**
- **All DTOs and data models** in `model/` package
- Documentation: **Russian**, Code/comments/commits: **English**
