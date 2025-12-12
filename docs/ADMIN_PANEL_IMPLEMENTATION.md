# План реализации админ-панели Notifier Service

## 1. Анализ текущей библиотеки

### Возможности dehuckakpyt/telegram-bot (0.13.4)

| Компонент | Назначение | Поддержка авторизации |
|-----------|------------|----------------------|
| `TelegramUserSource` | Хранение пользователей | Нет (только `save(user, available)`) |
| `BotHandler` | Обработка команд/callbacks/steps | Нет ролей |
| `BotUpdateHandler` | Обработка любых updates | Вызывается ДО BotHandler |
| `ChatException` | Ошибки для пользователя | Можно использовать для access denied |

### Типы взаимодействий в Telegram

| Тип | Метод в BotUpdateHandler | Пример | Как авторизовать |
|-----|--------------------------|--------|------------------|
| **Commands** | `message {}` | `/start`, `/list_users` | По имени команды |
| **Callbacks** | `callbackQuery {}` | Нажатие inline кнопки | По префиксу callback data |
| **Steps** | `message {}` | Текст в цепочке диалога | По текущему шагу пользователя |
| **Inline queries** | `inlineQuery {}` | `@bot запрос` | По userId |

### Ключевой вывод

**Библиотека НЕ имеет встроенной системы авторизации.** Необходимо реализовать проверку для ВСЕХ типов взаимодействий.

---

## 2. Архитектура решения

### 2.1 Подход: SecureBotHandler с Enum-based permissions

```
Telegram Update
      ↓
SecureBotHandler
      ↓
  command(BotCommands.LIST_USERS)  ← enum содержит роль
      ↓
  [Автоматическая проверка прав]
      ↓
    DENY → SilentChatException
      ↓
   ALLOW → выполнение action
```

**Почему SecureBotHandler:**
- Enum — единственный способ объявить handler (нельзя забыть права)
- Type-safe — компилятор не даст использовать несуществующую команду
- Единый источник правды — команда + описание + роль в одном месте
- Не нужен отдельный BotUpdateHandler для авторизации

### 2.2 Модель данных

```
┌─────────────────────────────────────┐
│           bot_user                   │
├─────────────────────────────────────┤
│ id            BIGSERIAL PRIMARY KEY  │
│ telegram_id   BIGINT UNIQUE NOT NULL │
│ username      VARCHAR(32)            │
│ first_name    VARCHAR(64)            │
│ last_name     VARCHAR(64)            │
│ role          VARCHAR(16) NOT NULL   │  -- USER, ADMIN
│ active        BOOLEAN DEFAULT true   │
│ created_at    TIMESTAMP              │
│ updated_at    TIMESTAMP              │
└─────────────────────────────────────┘
```

**Отличие от референсного проекта:**
- НЕ используем chatId (в private chat telegramId == chatId)
- Роль хранится как enum, не Set (достаточно одной роли с иерархией)
- Активация происходит сразу при добавлении (не двухэтапная)

---

## 3. Компоненты

### 3.1 Слой данных

```
model/
├── UserRole.kt           # enum: USER, ADMIN
└── BotUserEntity.kt      # JPA entity

repository/
└── BotUserRepository.kt  # Spring Data JPA
```

### 3.2 Сервисный слой

```
service/
├── BotUserService.kt           # CRUD операции
├── AuthorizationService.kt     # Проверка прав
└── BotCommandMenuService.kt    # Динамическое меню команд
```

### 3.3 Secure Handler Framework

```
handler/
├── SecureBotHandler.kt         # Базовый класс с авторизацией
├── SecureBotHandling.kt        # DSL обёртка над BotHandling
└── SilentChatException.kt      # Exception для silent deny

bot/
├── BotCommands.kt              # Enum команд с ролями
├── BotCallbacks.kt             # Enum callbacks с ролями
└── BotSteps.kt                 # Enum steps с ролями

config/
└── AdminProperties.kt          # Master admin config
```

### 3.4 Обработчики команд

```
handler/
├── StartHandler.kt             # /start (public)
├── HelpHandler.kt              # /help (USER+)
├── SettingsHandler.kt          # /settings (USER+)
└── admin/
    ├── ListUsersHandler.kt     # /list_users (ADMIN)
    ├── AddUserHandler.kt       # /add_user (ADMIN)
    └── RemoveUserHandler.kt    # /remove_user (ADMIN)
```

---

## 4. Детальная реализация

### 4.1 UserRole

```kotlin
enum class UserRole {
    USER,
    ADMIN;

    fun hasPermission(required: UserRole): Boolean =
        this.ordinal >= required.ordinal
}
```

**Иерархия:** `ADMIN > USER > (none)`

### 4.2 BotUserEntity

```kotlin
@Entity
@Table(name = "bot_user")
class BotUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "telegram_id", unique = true, nullable = false)
    val telegramId: Long,

    @Column(length = 32)
    var username: String? = null,

    @Column(name = "first_name", length = 64)
    var firstName: String? = null,

    @Column(name = "last_name", length = 64)
    var lastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    var role: UserRole = UserRole.USER,

    @Column(nullable = false)
    var active: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
```

### 4.3 AuthorizationService

```kotlin
@Service
class AuthorizationService(
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    fun isAuthorized(telegramId: Long, requiredRole: UserRole?): Boolean {
        // Master admin bypass
        if (telegramId == adminProperties.masterAdminId) return true

        // Public commands
        if (requiredRole == null) return true

        // Check user role
        val user = botUserRepository.findByTelegramId(telegramId)
            ?: return false

        return user.active && user.role.hasPermission(requiredRole)
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)
}
```

### 4.4 BotCommands — Enum команд с ролями

```kotlin
enum class BotCommands(
    val command: String,
    val description: String,
    val requiredRole: UserRole? = null  // null = public
) {
    // Public
    START("/start", "Начать работу"),

    // User commands
    HELP("/help", "Показать справку", UserRole.USER),
    SETTINGS("/settings", "Настройки уведомлений", UserRole.USER),

    // Admin commands
    LIST_USERS("/list_users", "Список пользователей", UserRole.ADMIN),
    ADD_USER("/add_user", "Добавить пользователя", UserRole.ADMIN),
    REMOVE_USER("/remove_user", "Удалить пользователя", UserRole.ADMIN);

    val slashCommand: String get() = command
}
```

### 4.5 BotCallbacks — Enum callbacks с ролями

```kotlin
enum class BotCallbacks(
    val callback: String,
    val requiredRole: UserRole? = UserRole.USER  // default: USER
) {
    // User callbacks
    SETTINGS_NOTIFICATIONS("settings:notifications"),
    SETTINGS_QUIET_HOURS("settings:quiet_hours"),
    SETTINGS_CANCEL("settings:cancel"),
    NOTIFY_ALL("notify:all"),
    NOTIFY_IMPORTANT("notify:important"),
    NOTIFY_OFF("notify:off"),

    // Admin callbacks
    ADMIN_ADD_BY_ID("admin:add:by_id", UserRole.ADMIN),
    ADMIN_ADD_BY_USERNAME("admin:add:by_username", UserRole.ADMIN),
    ADMIN_CANCEL("admin:cancel", UserRole.ADMIN),
    ROLE_USER("role:USER", UserRole.ADMIN),
    ROLE_ADMIN("role:ADMIN", UserRole.ADMIN);

    // Для динамических callbacks (admin:remove:123)
    companion object {
        fun findByPrefix(data: String): BotCallbacks? =
            entries.find { data.startsWith(it.callback) }
    }
}
```

### 4.6 BotSteps — Enum steps с ролями

```kotlin
enum class BotSteps(
    val step: String,
    val requiredRole: UserRole = UserRole.USER
) {
    // User steps
    GET_QUIET_START("get_quiet_start"),
    GET_QUIET_END("get_quiet_end"),

    // Admin steps
    GET_USER_ID("get_user_id", UserRole.ADMIN),
    SELECT_ROLE("select_role", UserRole.ADMIN),
    GET_USERNAME("get_username", UserRole.ADMIN);
}
```

### 4.7 SecureBotHandling — DSL обёртка

```kotlin
class SecureBotHandling(
    private val handling: BotHandling,
    private val auth: AuthorizationService
) {
    val bot: TelegramBot get() = handling.bot

    // ===== Commands =====
    fun command(
        cmd: BotCommands,
        next: String? = null,
        action: suspend CommandContainer.() -> Unit
    ) {
        handling.command(cmd.command, next) {
            if (cmd.requiredRole != null && !auth.isAuthorized(from.id, cmd.requiredRole)) {
                throw SilentChatException()
            }
            action()
        }
    }

    // ===== Callbacks =====
    fun callback(
        cb: BotCallbacks,
        next: String? = null,
        action: suspend CallbackContainer.() -> Unit
    ) {
        handling.callback(cb.callback, next) {
            if (cb.requiredRole != null && !auth.isAuthorized(from.id, cb.requiredRole)) {
                bot.answerCallbackQuery(callbackQueryId = callbackQueryId, text = "Доступ запрещён")
                throw SilentChatException()
            }
            action()
        }
    }

    // Для динамических callbacks с regex (admin:remove:\d+)
    fun callbackRegex(
        pattern: Regex,
        requiredRole: UserRole? = UserRole.USER,
        next: String? = null,
        action: suspend CallbackContainer.(MatchResult) -> Unit
    ) {
        handling.callback(pattern.pattern, next) {
            if (requiredRole != null && !auth.isAuthorized(from.id, requiredRole)) {
                bot.answerCallbackQuery(callbackQueryId = callbackQueryId, text = "Доступ запрещён")
                throw SilentChatException()
            }
            val match = pattern.find(callbackData) ?: return@callback
            action(match)
        }
    }

    // ===== Steps =====
    fun step(
        st: BotSteps,
        next: String? = null,
        action: suspend TextMessageContainer.() -> Unit
    ) {
        handling.step(st.step, next) {
            if (!auth.isAuthorized(from.id, st.requiredRole)) {
                throw SilentChatException()
            }
            action()
        }
    }

    // ===== Утилиты (делегирование) =====
    fun GeneralContainer.next(step: String?) {
        handling.run { this@next.next(step) }
    }

    fun GeneralContainer.next(step: BotSteps) {
        handling.run { this@next.next(step.step) }
    }

    fun GeneralContainer.transfer(instance: Any) {
        handling.run { this@transfer.transfer(instance) }
    }

    inline fun <reified T> GeneralContainer.transferred(): T =
        handling.run { this@transferred.transferred() }
}
```

### 4.8 SecureBotHandler — Базовый класс

```kotlin
abstract class SecureBotHandler(
    auth: AuthorizationService,
    block: SecureBotHandling.() -> Unit
) : BotHandler({
    SecureBotHandling(this, auth).block()
})
```

### 4.9 SilentChatException

```kotlin
class SilentChatException : ChatException("") {
    // Переопределить в ExceptionHandler для silent handling
}
```

### 4.7 AdminProperties

```kotlin
@ConfigurationProperties(prefix = "notifier.admin")
data class AdminProperties(
    val masterAdminId: Long
)
```

```yaml
# application.yml
notifier:
  admin:
    master-admin-id: ${MASTER_ADMIN_ID}
```

### 4.8 BotCommandMenuService - Динамическое меню команд

Telegram позволяет устанавливать персонализированное меню команд для каждого пользователя через `setMyCommands` с `BotCommandScopeChat`.

```kotlin
@Service
class BotCommandMenuService(
    private val telegramBot: TelegramBot,
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateCommandsForUser(telegramId: Long, role: UserRole) {
        val commands = BotCommands.entries
            .filter { cmd ->
                cmd.requiredRole == null || role.hasPermission(cmd.requiredRole)
            }
            .map { BotCommand(it.command, it.description) }

        telegramBot.setMyCommands(
            commands = commands,
            scope = BotCommandScopeChat(chatId = telegramId.toString())
        )

        logger.debug("Updated commands for user $telegramId: ${commands.map { it.command }}")
    }

    suspend fun resetCommandsForUser(telegramId: Long) {
        telegramBot.setMyCommands(
            commands = getPublicCommands(),
            scope = BotCommandScopeChat(chatId = telegramId.toString())
        )
    }

    suspend fun initializeAllCommands() {
        // 1. Глобальные команды (для неавторизованных)
        telegramBot.setMyCommands(commands = getPublicCommands())

        // 2. Master admin - все команды
        telegramBot.setMyCommands(
            commands = BotCommands.entries.map { BotCommand(it.command, it.description) },
            scope = BotCommandScopeChat(chatId = adminProperties.masterAdminId.toString())
        )

        // 3. Каждому активному пользователю по его роли
        botUserRepository.findAllByActiveTrue().forEach { user ->
            updateCommandsForUser(user.telegramId, user.role)
        }

        logger.info("Bot commands initialized for all users")
    }

    private fun getPublicCommands(): List<BotCommand> =
        BotCommands.entries
            .filter { it.requiredRole == null }
            .map { BotCommand(it.command, it.description) }
}
```

### 4.9 BotCommands с ролями

```kotlin
enum class BotCommands(
    val command: String,
    val description: String,
    val requiredRole: UserRole? = null
) {
    START("start", "Начать работу", null),
    HELP("help", "Показать справку", UserRole.USER),
    SETTINGS("settings", "Настройки уведомлений", UserRole.USER),
    LIST_USERS("list_users", "Список пользователей", UserRole.ADMIN),
    ADD_USER("add_user", "Добавить пользователя", UserRole.ADMIN),
    REMOVE_USER("remove_user", "Удалить пользователя", UserRole.ADMIN);

    val slashCommand: String get() = "/$command"

    companion object {
        fun all(): List<BotCommand> = entries.map {
            BotCommand(it.command, it.description)
        }
    }
}
```

### 4.10 Инициализация меню при старте

```kotlin
@Configuration
@EnableTelegramBot
class BotConfig(
    private val telegramBot: TelegramBot,
    private val botCommandMenuService: BotCommandMenuService
) {
    @PostConstruct
    fun init() = runBlocking {
        botCommandMenuService.initializeAllCommands()
    }
}
```

### 4.11 Обновление меню при изменении пользователя

Вызывать `botCommandMenuService` в:

| Событие | Действие |
|---------|----------|
| Добавление пользователя | `updateCommandsForUser(telegramId, role)` |
| Изменение роли | `updateCommandsForUser(telegramId, newRole)` |
| Удаление пользователя | `resetCommandsForUser(telegramId)` |
| Активация через /start | `updateCommandsForUser(telegramId, role)` |

---

## 5. Админ-команды

### 5.1 /list_users

```kotlin
@HandlerComponent
class ListUsersHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService
) : SecureBotHandler(auth, {

    command(BotCommands.LIST_USERS) {  // автоматически требует ADMIN
        val users = botUserService.getAllUsers()

        if (users.isEmpty()) {
            sendMessage("Пользователей нет")
            return@command
        }

        val text = buildString {
            appendLine("📋 Пользователи (${users.size}):")
            appendLine()
            users.forEachIndexed { i, user ->
                val status = if (user.active) "✅" else "❌"
                val name = user.username?.let { "@$it" }
                    ?: user.firstName
                    ?: user.telegramId.toString()
                appendLine("${i + 1}. $name $status")
                appendLine("   Роль: ${user.role.name.lowercase()}")
                appendLine("   ID: ${user.telegramId}")
            }
        }

        sendMessage(text)
    }
})
```

### 5.2 /add_user

```kotlin
@HandlerComponent
class AddUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService
) : SecureBotHandler(auth, {

    command(BotCommands.ADD_USER) {  // ADMIN
        sendMessage(
            "Как добавить пользователя?",
            replyMarkup = inlineKeyboard(
                callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                callbackButton("По username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    callback(BotCallbacks.ADMIN_ADD_BY_ID, next = BotSteps.GET_USER_ID.step) {  // ADMIN
        sendMessage("Введи Telegram ID пользователя:")
    }

    step(BotSteps.GET_USER_ID, next = BotSteps.SELECT_ROLE.step) {  // ADMIN
        val telegramId = text.toLongOrNull()
            ?: throw ChatException("Некорректный ID. Введи число.")

        if (botUserService.existsByTelegramId(telegramId)) {
            throw ChatException("Пользователь уже существует")
        }

        transfer(telegramId)

        sendMessage(
            "Выбери роль:",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    callback(BotCallbacks.ROLE_USER) {  // ADMIN
        val telegramId = transferred<Long>()
        botUserService.create(telegramId, UserRole.USER)
        sendMessage("✅ Пользователь добавлен с ролью USER")
    }

    callback(BotCallbacks.ROLE_ADMIN) {  // ADMIN
        val telegramId = transferred<Long>()
        botUserService.create(telegramId, UserRole.ADMIN)
        sendMessage("✅ Пользователь добавлен с ролью ADMIN")
    }

    callback(BotCallbacks.ADMIN_ADD_BY_USERNAME, next = BotSteps.GET_USERNAME.step) {  // ADMIN
        sendMessage("Введи @username:")
    }

    step(BotSteps.GET_USERNAME) {  // ADMIN
        val username = text.removePrefix("@")

        if (!username.matches(Regex("^[a-zA-Z0-9_]{5,32}$"))) {
            throw ChatException("Некорректный username")
        }

        botUserService.createByUsername(username, UserRole.USER)
        sendMessage("✅ Пользователь @$username добавлен")
    }

    callback(BotCallbacks.ADMIN_CANCEL) {  // ADMIN
        sendMessage("Отменено")
    }
})
```

### 5.3 /remove_user

```kotlin
@HandlerComponent
class RemoveUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val adminProperties: AdminProperties
) : SecureBotHandler(auth, {

    command(BotCommands.REMOVE_USER) {  // ADMIN
        val users = botUserService.getAllUsers()
            .filter { it.telegramId != adminProperties.masterAdminId }

        if (users.isEmpty()) {
            sendMessage("Нет пользователей для удаления")
            return@command
        }

        sendMessage(
            "Выбери пользователя для удаления:",
            replyMarkup = inlineKeyboard(
                *users.map { user ->
                    val name = user.username?.let { "@$it" }
                        ?: user.telegramId.toString()
                    callbackButton(name, "admin:remove:${user.telegramId}")
                }.toTypedArray(),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    // Динамический callback с regex
    callbackRegex(Regex("admin:remove:(\\d+)"), UserRole.ADMIN) { match ->
        val telegramId = match.groupValues[1].toLong()

        sendMessage(
            "Удалить пользователя?",
            replyMarkup = inlineKeyboard(
                callbackButton("✅ Да", "admin:confirm_remove:$telegramId"),
                callbackButton("❌ Нет", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    callbackRegex(Regex("admin:confirm_remove:(\\d+)"), UserRole.ADMIN) { match ->
        val telegramId = match.groupValues[1].toLong()
        botUserService.delete(telegramId)
        sendMessage("✅ Пользователь удалён")
    }
})
```

---

## 6. Миграции БД

### V2__create_bot_user_table.sql

```sql
CREATE TABLE bot_user (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE NOT NULL,
    username      VARCHAR(32),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    role          VARCHAR(16) NOT NULL DEFAULT 'USER',
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bot_user_username ON bot_user(username);
CREATE INDEX idx_bot_user_role ON bot_user(role);
```

---

## 7. Обновление StartHandler

```kotlin
@HandlerComponent
class StartHandler(
    private val botUserService: BotUserService,
    private val authorizationService: AuthorizationService
) : BotHandler({

    command("/start") {
        val telegramId = from.id
        val existingUser = authorizationService.getUser(telegramId)

        when {
            existingUser != null -> {
                // Обновляем данные пользователя
                botUserService.updateUserInfo(
                    telegramId = telegramId,
                    username = from.username,
                    firstName = from.firstName,
                    lastName = from.lastName
                )
                sendMessage("С возвращением! Используй /help для списка команд.")
            }

            // Проверяем, был ли добавлен по username
            from.username != null -> {
                val pendingUser = botUserService.findByUsername(from.username!!)
                if (pendingUser != null) {
                    botUserService.activateByUsername(
                        username = from.username!!,
                        telegramId = telegramId,
                        firstName = from.firstName,
                        lastName = from.lastName
                    )
                    sendMessage("Добро пожаловать! Используй /help для списка команд.")
                } else {
                    sendMessage("У тебя нет доступа. Обратись к администратору.")
                }
            }

            else -> {
                sendMessage("У тебя нет доступа. Обратись к администратору.")
            }
        }
    }
})
```

---

## 8. Конфигурация

### application.yml

```yaml
notifier:
  admin:
    master-admin-id: ${MASTER_ADMIN_ID}

telegram-bot:
  token: ${TELEGRAM_BOT_TOKEN}
  username: ${TELEGRAM_BOT_USERNAME}
  source-jpa:
    enabled: true
    chain-source.enabled: true
    callback-content-source.enabled: true
    user-source.enabled: false  # Используем свою реализацию
```

---

## 9. Структура файлов

```
src/main/kotlin/com/smhomelab/notifier/
├── NotifierApplication.kt
├── config/
│   ├── BotConfig.kt
│   └── AdminProperties.kt
├── model/
│   ├── UserRole.kt
│   └── BotUserEntity.kt
├── repository/
│   └── BotUserRepository.kt
├── service/
│   ├── BotUserService.kt
│   ├── AuthorizationService.kt
│   └── BotCommandMenuService.kt
├── bot/
│   ├── BotCommands.kt              # Enum команд с ролями
│   ├── BotCallbacks.kt             # Enum callbacks с ролями
│   └── BotSteps.kt                 # Enum steps с ролями
├── handler/
│   ├── SecureBotHandler.kt         # Базовый класс
│   ├── SecureBotHandling.kt        # DSL обёртка
│   ├── SilentChatException.kt
│   ├── StartHandler.kt
│   ├── HelpHandler.kt
│   ├── SettingsHandler.kt
│   └── admin/
│       ├── ListUsersHandler.kt
│       ├── AddUserHandler.kt
│       └── RemoveUserHandler.kt
```

---

## 9.1 Диаграмма авторизации через SecureBotHandler

```
┌──────────────────────────────────────────────────────────────────────────┐
│                          Telegram Update                                   │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                         SecureBotHandler                                   │
│                    (extends BotHandler)                                    │
└──────────────────────────────────────────────────────────────────────────┘
                                    │
            ┌───────────────────────┼───────────────────────┐
            ▼                       ▼                       ▼
    ┌───────────────┐      ┌───────────────┐      ┌───────────────┐
    │   command()    │      │  callback()   │      │    step()     │
    │ (BotCommands)  │      │(BotCallbacks) │      │  (BotSteps)   │
    └───────────────┘      └───────────────┘      └───────────────┘
            │                       │                       │
            ▼                       ▼                       ▼
    ┌───────────────┐      ┌───────────────┐      ┌───────────────┐
    │ enum.required │      │ enum.required │      │ enum.required │
    │ Role → ADMIN  │      │ Role → ADMIN  │      │ Role → ADMIN  │
    └───────────────┘      └───────────────┘      └───────────────┘
            │                       │                       │
            └───────────────────────┼───────────────────────┘
                                    ▼
                    ┌───────────────────────────────┐
                    │     AuthorizationService      │
                    │   isAuthorized(id, role)?     │
                    └───────────────────────────────┘
                                    │
                    ┌───────────────┴───────────────┐
                    ▼                               ▼
            ┌───────────────┐              ┌───────────────┐
            │   ALLOWED     │              │    DENIED     │
            │       ↓       │              │       ↓       │
            │    action()   │              │ SilentException│
            │  (логика)     │              │               │
            └───────────────┘              └───────────────┘
```

**Ключевое отличие:** Права определены в enum, нельзя объявить handler без указания enum → невозможно забыть проверку.

---

## 10. Отличия от референсного проекта

| Аспект | Референс | Наша реализация |
|--------|----------|-----------------|
| Библиотека | telegrambots:6.9.7.1 | dehuckakpyt:0.13.4 |
| Авторизация | Только команды | **Все типы**: команды, callbacks, steps |
| Точка авторизации | В каждом handler вручную | **SecureBotHandler** — автоматически через enum |
| Роли | Set<UserRole> | Одна роль с иерархией |
| Permissions | Map строк на роли | **Enum-based** (BotCommands, BotCallbacks, BotSteps) |
| Type-safety | Строки — можно ошибиться | **Compile-time** — enum обязателен |
| Забыть проверку | Возможно | **Невозможно** — нет raw command(String) |
| Регистрация | Двухэтапная (add → /start) | Опционально двухэтапная |
| Меню команд | Динамическое по ролям | Динамическое через BotCommandMenuService |
| Silent deny | return без ответа | SilentChatException + answerCallbackQuery |

---

## 11. Безопасность

### Реализовано
- Master admin через env переменную
- **Enum-based авторизация** — невозможно объявить handler без указания роли
- **Compile-time safety** — компилятор не даст использовать несуществующую команду
- Авторизация ВСЕХ типов взаимодействий (команды, callbacks, steps)
- Проверка callbacks через enum и regex с указанием роли
- Подтверждение удаления пользователя
- Валидация входных данных

### Векторы атак закрыты

| Вектор | Защита |
|--------|--------|
| Прямой вызов admin команды | `command(BotCommands.LIST_USERS)` → enum содержит `ADMIN` |
| Перехват callback data | `callback(BotCallbacks.ADMIN_ADD_BY_ID)` → enum содержит `ADMIN` |
| Динамический callback | `callbackRegex(pattern, UserRole.ADMIN)` → роль обязательна |
| Step в admin flow | `step(BotSteps.GET_USER_ID)` → enum содержит `ADMIN` |
| Забыть проверку | **Невозможно** — raw `command(String)` недоступен |

### Рекомендации на будущее
- [ ] Audit log административных действий
- [ ] Rate limiting для команд
- [ ] Уведомление при добавлении/удалении пользователя
