# Notifier Service

## Architecture

```
Handler → Service → Facade → Repository
```

- **Handler**: bot commands/callbacks, knows only Service
- **Service**: suspend business logic, knows only Facade
- **Facade**: sync, @Transactional, knows only Repository
- **Repository**: JPA

## Package Structure

```
persistence/
  model/       # JPA entities
  repository/  # Spring Data JPA
  facade/      # sync, @Transactional
service/       # suspend business logic
handler/       # bot handlers
bot/           # enums (commands, callbacks, steps)
config/        # Spring config
```

## Tech Stack

- Kotlin + Spring Boot
- `dehuckakpyt/telegram-bot:0.13.4`
- PostgreSQL + Flyway
