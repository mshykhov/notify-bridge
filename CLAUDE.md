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

## Library Sources

- **telegram-bot lib**: `/mnt/c/Users/Myron/IdeaProjects/telegram-bot` (local clone for reference)

## Future: View Layer

When 3+ similar menus exist, refactor to View layer:

```
handler/           # only sends messages
view/              # builds UI (text + buttons)
service/           # business logic
```

```kotlin
// view/ViewData.kt
data class ButtonData(val text: String, val callback: String)
data class MessageView(val text: String, val buttons: List<List<ButtonData>>)

// view/SettingsView.kt
@Component
class SettingsView(private val settingsService: SettingsService) {
    fun mainMenu(telegramId: Long): MessageView { ... }
    fun pushoverMenu(telegramId: Long): MessageView { ... }
}

// handler - thin, only sends
secureCommand(BotCommands.SETTINGS, auth) {
    sendView(settingsView.mainMenu(from.id))
}
```

Benefits: testable views, thin handlers, reusable UI components.
