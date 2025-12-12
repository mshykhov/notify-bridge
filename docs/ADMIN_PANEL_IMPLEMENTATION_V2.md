# План реализации админ-панели Notifier Service (v2)

## 1. Анализ библиотеки dehuckakpyt/telegram-bot (0.13.4)

| Компонент | Назначение | Поддержка авторизации |
|-----------|------------|----------------------|
| `BotHandler` | Обработка команд/callbacks/steps | Нет ролей |
| `BotUpdateHandler` | Обработка любых updates | Вызывается ДО BotHandler |
| `ChatException` | Ошибки для пользователя | Отправляет сообщение |
| `ExceptionHandler` | Кастомная обработка ошибок | Для silent deny |

**Вывод:** Библиотека НЕ имеет встроенной системы авторизации. Реализуем через SecureBotHandler + Enum-based permissions.

---

## 2. Архитектура

### 2.1 Подход: SecureBotHandler с Enum-based permissions

```
Telegram Update
      │
      ▼
SecureBotHandler
      │
      ▼
command(BotCommands.LIST_USERS)  ← enum содержит роль
      │
      ▼
[Автоматическая проверка прав]
      │
      ├─ DENY → SilentChatException
      │
      └─ ALLOW → выполнение action
```

**Преимущества:**
- Enum — единственный способ объявить handler (нельзя забыть права)
- Type-safe — компилятор не даст использовать несуществующую команду
- Единый источник правды — команда + описание + роль в одном месте

### 2.2 Модель данных

```sql
CREATE TABLE bot_user (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE NOT NULL,
    username      VARCHAR(32),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    role          VARCHAR(16) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bot_user_telegram_id ON bot_user(telegram_id);
CREATE INDEX idx_bot_user_username ON bot_user(username);
```

---

## 3. Структура файлов

```
src/main/kotlin/com/smhomelab/notifier/
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
│   ├── BotCommands.kt
│   ├── BotCallbacks.kt
│   └── BotSteps.kt
├── handler/
│   ├── SecureBotHandler.kt
│   ├── SecureBotHandling.kt
│   ├── SilentChatException.kt
│   ├── SilentExceptionHandler.kt
│   ├── StartHandler.kt
│   ├── HelpHandler.kt
│   ├── SettingsHandler.kt
│   └── admin/
│       ├── ListUsersHandler.kt
│       ├── AddUserHandler.kt
│       └── RemoveUserHandler.kt
```

---

## 4. Реализация

### 4.1 UserRole

```kotlin
enum class UserRole(val level: Int) {
    USER(1),
    ADMIN(100);

    fun hasPermission(required: UserRole): Boolean = level >= required.level
}
```

**Примечание:** Используем `level` вместо `ordinal()` для надёжности при изменении порядка enum.

### 4.2 BotUserEntity

```kotlin
@Entity
@Table(name = "bot_user")
@EntityListeners(AuditingEntityListener::class)
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

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

### 4.3 BotUserRepository

```kotlin
@Repository
interface BotUserRepository : JpaRepository<BotUserEntity, Long> {
    fun findByTelegramId(telegramId: Long): BotUserEntity?
    fun findByUsernameIgnoreCase(username: String): BotUserEntity?
    fun findAllByActiveTrue(): List<BotUserEntity>
    fun existsByTelegramId(telegramId: Long): Boolean
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun deleteByTelegramId(telegramId: Long)
}
```

### 4.4 AdminProperties

```kotlin
@ConfigurationProperties(prefix = "notifier.admin")
data class AdminProperties(
    val masterAdminId: Long
)
```

```yaml
notifier:
  admin:
    master-admin-id: ${MASTER_ADMIN_ID}
```

### 4.5 AuthorizationService

```kotlin
@Service
class AuthorizationService(
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    fun isAuthorized(telegramId: Long, requiredRole: UserRole?): Boolean {
        if (telegramId == adminProperties.masterAdminId) return true
        if (requiredRole == null) return true

        val user = botUserRepository.findByTelegramId(telegramId) ?: return false
        return user.active && user.role.hasPermission(requiredRole)
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)

    fun isMasterAdmin(telegramId: Long): Boolean =
        telegramId == adminProperties.masterAdminId
}
```

### 4.6 BotUserService

```kotlin
@Service
@Transactional
class BotUserService(
    private val botUserRepository: BotUserRepository,
    private val botCommandMenuService: BotCommandMenuService,
    private val adminProperties: AdminProperties,
    private val telegramBot: TelegramBot
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllUsers(): List<BotUserEntity> =
        botUserRepository.findAll()
            .filter { it.telegramId != adminProperties.masterAdminId }

    fun create(telegramId: Long, role: UserRole): BotUserEntity {
        require(telegramId > 0) { "Invalid telegram ID" }
        require(!botUserRepository.existsByTelegramId(telegramId)) { "User already exists" }

        val user = botUserRepository.save(
            BotUserEntity(telegramId = telegramId, role = role)
        )

        runBlocking { botCommandMenuService.updateCommandsForUser(telegramId, role) }
        notifyUserAboutAccess(telegramId)

        logger.info("User created: telegramId=$telegramId, role=$role")
        return user
    }

    fun createByUsername(username: String, role: UserRole): BotUserEntity {
        require(!botUserRepository.existsByUsernameIgnoreCase(username)) { "User already exists" }

        val user = botUserRepository.save(
            BotUserEntity(telegramId = 0, username = username, role = role, active = false)
        )

        logger.info("User created by username: $username, role=$role (pending activation)")
        return user
    }

    fun delete(telegramId: Long) {
        require(telegramId != adminProperties.masterAdminId) { "Cannot delete master admin" }

        botUserRepository.deleteByTelegramId(telegramId)
        runBlocking { botCommandMenuService.resetCommandsForUser(telegramId) }

        logger.info("User deleted: telegramId=$telegramId")
    }

    fun activateByUsername(username: String, telegramId: Long, firstName: String?, lastName: String?): BotUserEntity? {
        val user = botUserRepository.findByUsernameIgnoreCase(username) ?: return null

        user.apply {
            this.firstName = firstName
            this.lastName = lastName
            this.active = true
        }

        val activated = botUserRepository.save(user)
        runBlocking { botCommandMenuService.updateCommandsForUser(telegramId, user.role) }

        logger.info("User activated: username=$username, telegramId=$telegramId")
        return activated
    }

    fun updateUserInfo(telegramId: Long, username: String?, firstName: String?, lastName: String?) {
        val user = botUserRepository.findByTelegramId(telegramId) ?: return
        user.apply {
            this.username = username
            this.firstName = firstName
            this.lastName = lastName
        }
        botUserRepository.save(user)
    }

    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserRepository.existsByTelegramId(telegramId)

    fun findByUsername(username: String): BotUserEntity? =
        botUserRepository.findByUsernameIgnoreCase(username)

    private fun notifyUserAboutAccess(telegramId: Long) {
        try {
            runBlocking {
                telegramBot.sendMessage(
                    chatId = telegramId,
                    text = "Тебе предоставлен доступ к боту. Используй /start"
                )
            }
        } catch (e: Exception) {
            logger.debug("Could not notify user $telegramId: ${e.message}")
        }
    }
}
```

### 4.7 BotCommands

```kotlin
enum class BotCommands(
    val command: String,
    val description: String,
    val requiredRole: UserRole? = null
) {
    START("start", "Начать работу"),
    HELP("help", "Показать справку", UserRole.USER),
    SETTINGS("settings", "Настройки уведомлений", UserRole.USER),
    LIST_USERS("list_users", "Список пользователей", UserRole.ADMIN),
    ADD_USER("add_user", "Добавить пользователя", UserRole.ADMIN),
    REMOVE_USER("remove_user", "Удалить пользователя", UserRole.ADMIN);

    val slashCommand: String get() = "/$command"

    fun toBotCommand() = BotCommand(command, description)

    companion object {
        fun all() = entries.map { it.toBotCommand() }
        fun forRole(role: UserRole?) = entries.filter { cmd ->
            cmd.requiredRole == null || (role != null && role.hasPermission(cmd.requiredRole))
        }
    }
}
```

### 4.8 BotCallbacks

```kotlin
enum class BotCallbacks(
    val callback: String,
    val requiredRole: UserRole = UserRole.USER
) {
    // User
    SETTINGS_NOTIFICATIONS("settings:notifications"),
    SETTINGS_QUIET_HOURS("settings:quiet_hours"),
    SETTINGS_CANCEL("settings:cancel"),
    NOTIFY_ALL("notify:all"),
    NOTIFY_IMPORTANT("notify:important"),
    NOTIFY_OFF("notify:off"),

    // Admin
    ADMIN_ADD_BY_ID("admin:add:by_id", UserRole.ADMIN),
    ADMIN_ADD_BY_USERNAME("admin:add:by_username", UserRole.ADMIN),
    ADMIN_CANCEL("admin:cancel", UserRole.ADMIN),
    ROLE_USER("role:USER", UserRole.ADMIN),
    ROLE_ADMIN("role:ADMIN", UserRole.ADMIN);
}
```

### 4.9 BotSteps

```kotlin
enum class BotSteps(
    val step: String,
    val requiredRole: UserRole = UserRole.USER
) {
    GET_QUIET_START("get_quiet_start"),
    GET_QUIET_END("get_quiet_end"),
    GET_USER_ID("get_user_id", UserRole.ADMIN),
    SELECT_ROLE("select_role", UserRole.ADMIN),
    GET_USERNAME("get_username", UserRole.ADMIN);
}
```

### 4.10 SilentChatException + Handler

```kotlin
class SilentChatException : ChatException("")

@Component
class SilentExceptionHandler : ExceptionHandler({
    throwable<SilentChatException> {
        // Silent - no response to user
    }
})
```

### 4.11 SecureBotHandling

```kotlin
class SecureBotHandling(
    private val handling: BotHandling,
    private val auth: AuthorizationService
) {
    val bot: TelegramBot get() = handling.bot

    fun command(cmd: BotCommands, next: String? = null, action: suspend CommandContainer.() -> Unit) {
        handling.command(cmd.slashCommand, next) {
            if (cmd.requiredRole != null && !auth.isAuthorized(from.id, cmd.requiredRole)) {
                throw SilentChatException()
            }
            action()
        }
    }

    fun callback(cb: BotCallbacks, next: String? = null, action: suspend CallbackContainer.() -> Unit) {
        handling.callback(cb.callback, next) {
            if (!auth.isAuthorized(from.id, cb.requiredRole)) {
                bot.answerCallbackQuery(callbackQueryId, text = "Доступ запрещён")
                throw SilentChatException()
            }
            action()
        }
    }

    fun callbackRegex(
        pattern: Regex,
        requiredRole: UserRole,
        next: String? = null,
        action: suspend CallbackContainer.(MatchResult) -> Unit
    ) {
        handling.callback(pattern.pattern, next) {
            if (!auth.isAuthorized(from.id, requiredRole)) {
                bot.answerCallbackQuery(callbackQueryId, text = "Доступ запрещён")
                throw SilentChatException()
            }
            val match = pattern.find(callbackData) ?: return@callback
            action(match)
        }
    }

    fun step(st: BotSteps, next: String? = null, action: suspend TextMessageContainer.() -> Unit) {
        handling.step(st.step, next) {
            if (!auth.isAuthorized(from.id, st.requiredRole)) {
                throw SilentChatException()
            }
            action()
        }
    }
}
```

### 4.12 SecureBotHandler

```kotlin
abstract class SecureBotHandler(
    auth: AuthorizationService,
    block: SecureBotHandling.() -> Unit
) : BotHandler({
    SecureBotHandling(this, auth).block()
})
```

### 4.13 BotCommandMenuService

```kotlin
@Service
class BotCommandMenuService(
    private val telegramBot: TelegramBot,
    private val botUserRepository: BotUserRepository,
    private val adminProperties: AdminProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun updateCommandsForUser(telegramId: Long, role: UserRole) {
        try {
            val commands = BotCommands.forRole(role).map { it.toBotCommand() }
            telegramBot.setMyCommands(
                commands = commands,
                scope = BotCommandScopeChat(chatId = telegramId.toString())
            )
            logger.debug("Updated commands for user $telegramId")
        } catch (e: TelegramApiException) {
            if (e.message?.contains("bot was blocked") == true) {
                logger.warn("User $telegramId blocked the bot")
                botUserRepository.findByTelegramId(telegramId)?.let {
                    it.active = false
                    botUserRepository.save(it)
                }
            } else {
                logger.error("Failed to update commands for $telegramId", e)
            }
        }
    }

    suspend fun resetCommandsForUser(telegramId: Long) {
        try {
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(null).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString())
            )
        } catch (e: Exception) {
            logger.debug("Could not reset commands for $telegramId")
        }
    }

    suspend fun initializeAllCommands() {
        // Global (public)
        telegramBot.setMyCommands(
            commands = BotCommands.forRole(null).map { it.toBotCommand() }
        )

        // Master admin - all commands
        telegramBot.setMyCommands(
            commands = BotCommands.all(),
            scope = BotCommandScopeChat(chatId = adminProperties.masterAdminId.toString())
        )

        // Each active user
        botUserRepository.findAllByActiveTrue().forEach { user ->
            updateCommandsForUser(user.telegramId, user.role)
        }

        logger.info("Bot commands initialized")
    }
}
```

---

## 5. Админ-команды

### 5.1 ListUsersHandler

```kotlin
@HandlerComponent
class ListUsersHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService
) : SecureBotHandler(auth, {

    command(BotCommands.LIST_USERS) {
        val users = botUserService.getAllUsers()

        if (users.isEmpty()) {
            sendMessage("Список пуст")
            return@command
        }

        val text = buildString {
            appendLine("📋 Пользователи (${users.size}):")
            appendLine()
            users.forEachIndexed { i, user ->
                val status = if (user.active) "✅" else "⏳"
                val name = user.username?.let { "@$it" } ?: user.telegramId.toString()
                appendLine("${i + 1}. $name $status")
                appendLine("   Роль: ${user.role.name.lowercase()}")
                appendLine("   ID: ${user.telegramId}")
                user.createdAt?.let { appendLine("   Добавлен: ${it.toString().take(10)}") }
            }
        }

        sendMessage(text)
    }
})
```

### 5.2 AddUserHandler

```kotlin
@HandlerComponent
class AddUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService
) : SecureBotHandler(auth, {

    command(BotCommands.ADD_USER) {
        sendMessage(
            "Как добавить пользователя?",
            replyMarkup = inlineKeyboard(
                callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                callbackButton("По username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    callback(BotCallbacks.ADMIN_ADD_BY_ID, next = BotSteps.GET_USER_ID.step) {
        sendMessage("Введи Telegram ID:")
    }

    step(BotSteps.GET_USER_ID, next = BotSteps.SELECT_ROLE.step) {
        val telegramId = text.toLongOrNull()?.takeIf { it > 0 }
            ?: throw ChatException("Некорректный ID")

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

    callback(BotCallbacks.ROLE_USER) {
        val telegramId = transferred<Long>()
        botUserService.create(telegramId, UserRole.USER)
        sendMessage("✅ Пользователь добавлен (USER)")
    }

    callback(BotCallbacks.ROLE_ADMIN) {
        val telegramId = transferred<Long>()
        botUserService.create(telegramId, UserRole.ADMIN)
        sendMessage("✅ Пользователь добавлен (ADMIN)")
    }

    callback(BotCallbacks.ADMIN_ADD_BY_USERNAME, next = BotSteps.GET_USERNAME.step) {
        sendMessage("Введи @username:")
    }

    step(BotSteps.GET_USERNAME) {
        val username = text.removePrefix("@")

        if (!username.matches(Regex("^[a-zA-Z0-9_]{5,32}$"))) {
            throw ChatException("Некорректный username")
        }

        if (botUserService.findByUsername(username) != null) {
            throw ChatException("Пользователь уже существует")
        }

        botUserService.createByUsername(username, UserRole.USER)
        sendMessage("✅ Пользователь @$username добавлен (активируется при /start)")
    }

    callback(BotCallbacks.ADMIN_CANCEL) {
        sendMessage("Отменено")
    }
})
```

### 5.3 RemoveUserHandler

```kotlin
@HandlerComponent
class RemoveUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService
) : SecureBotHandler(auth, {

    command(BotCommands.REMOVE_USER) {
        val users = botUserService.getAllUsers()

        if (users.isEmpty()) {
            sendMessage("Нет пользователей")
            return@command
        }

        sendMessage(
            "Выбери пользователя:",
            replyMarkup = inlineKeyboard(
                *users.map { user ->
                    val name = user.username?.let { "@$it" } ?: user.telegramId.toString()
                    callbackButton(name, "admin:remove:${user.telegramId}")
                }.toTypedArray(),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

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
        sendMessage("✅ Удалён")
    }
})
```

---

## 6. StartHandler

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
                botUserService.updateUserInfo(telegramId, from.username, from.firstName, from.lastName)
                sendMessage("С возвращением! /help для списка команд.")
            }

            from.username != null -> {
                val pending = botUserService.findByUsername(from.username!!)
                if (pending != null) {
                    botUserService.activateByUsername(from.username!!, telegramId, from.firstName, from.lastName)
                    sendMessage("Добро пожаловать! /help для списка команд.")
                } else {
                    sendMessage("Нет доступа. Обратись к администратору.")
                }
            }

            else -> sendMessage("Нет доступа. Обратись к администратору.")
        }
    }
})
```

---

## 7. Конфигурация

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
    user-source.enabled: false
```

### BotConfig

```kotlin
@Configuration
@EnableTelegramBot
@EnableJpaAuditing
@EnableConfigurationProperties(AdminProperties::class)
class BotConfig(
    private val botCommandMenuService: BotCommandMenuService
) {
    @PostConstruct
    fun init() = runBlocking {
        botCommandMenuService.initializeAllCommands()
    }
}
```

---

## 8. Миграция БД

### V2__create_bot_user_table.sql

```sql
CREATE TABLE bot_user (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE NOT NULL,
    username      VARCHAR(32),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    role          VARCHAR(16) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bot_user_telegram_id ON bot_user(telegram_id);
CREATE INDEX idx_bot_user_username ON bot_user(username);
```

---

## 9. Безопасность

### Реализовано
- Master admin через env переменную
- Enum-based авторизация (невозможно забыть проверку)
- Compile-time safety
- Авторизация всех типов: команды, callbacks, steps
- Подтверждение удаления
- Защита от удаления master admin
- Валидация telegramId (> 0)
- Case-insensitive поиск по username
- Обработка блокировки бота пользователем
- @Transactional на сервисном слое

### Закрытые векторы атак

| Вектор | Защита |
|--------|--------|
| Прямой вызов admin команды | `command(BotCommands.LIST_USERS)` требует ADMIN |
| Перехват callback data | `callback(BotCallbacks.*)` требует роль из enum |
| Динамический callback | `callbackRegex(pattern, UserRole.ADMIN)` |
| Step в admin flow | `step(BotSteps.GET_USER_ID)` требует ADMIN |
| Удаление master admin | Проверка в `BotUserService.delete()` |
| Невалидный telegramId | Проверка `> 0` при создании |

---

## 10. TODO на будущее

- [ ] Audit log административных действий
- [ ] Rate limiting для команд
- [ ] Пагинация в /list_users (если > 20 пользователей)
- [ ] Изменение роли существующего пользователя
- [ ] Bulk операции (удаление нескольких)
- [ ] Уведомление admin при новой регистрации
