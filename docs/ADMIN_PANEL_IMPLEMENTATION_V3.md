# План реализации админ-панели Notifier Service (v3)

## 1. Архитектура

### Подход: SecureBotHandler + Enum-based permissions + Invitations

```
┌─────────────────────────────────────────────────────────────┐
│                    Closed Registration Flow                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Admin: /add_user @username                                 │
│              │                                              │
│              ▼                                              │
│     ┌─────────────────┐                                     │
│     │ bot_user_invitation │  (username + role)              │
│     └─────────────────┘                                     │
│              │                                              │
│              │  User: /start                                │
│              ▼                                              │
│     ┌─────────────────┐                                     │
│     │    bot_user     │  (telegramId + role)                │
│     └─────────────────┘                                     │
│              │                                              │
│              ▼                                              │
│     Invitation удаляется                                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**Преимущества:**
- `bot_user.telegram_id` всегда NOT NULL
- Чистая модель данных
- Нет хаков с telegramId = 0 или отрицательными ID

---

## 2. Модель данных

### 2.1 Схема БД

```sql
-- V2__create_bot_user_tables.sql

CREATE TABLE bot_user (
    id            BIGSERIAL PRIMARY KEY,
    telegram_id   BIGINT UNIQUE NOT NULL,
    username      VARCHAR(32),
    first_name    VARCHAR(64),
    last_name     VARCHAR(64),
    role          VARCHAR(16) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bot_user_invitation (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(32) UNIQUE NOT NULL,
    role          VARCHAR(16) NOT NULL DEFAULT 'USER',
    created_by    BIGINT NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Примечание:** Индексы не нужны — UNIQUE автоматически создаёт индекс.

### 2.2 BotUserEntity

```kotlin
@Entity
@Table(name = "bot_user")
@EntityListeners(AuditingEntityListener::class)
class BotUserEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "telegram_id", unique = true, nullable = false)
    val telegramId: Long,  // val, NOT NULL — всегда есть

    @Column(length = 32)
    var username: String? = null,

    @Column(name = "first_name", length = 64)
    var firstName: String? = null,

    @Column(name = "last_name", length = 64)
    var lastName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    var role: UserRole = UserRole.USER,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
)
```

### 2.3 BotUserInvitation

```kotlin
@Entity
@Table(name = "bot_user_invitation")
class BotUserInvitation(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false, length = 32)
    val username: String,

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    val role: UserRole = UserRole.USER,

    @Column(name = "created_by", nullable = false)
    val createdBy: Long,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant? = null
)
```

### 2.4 AddUserData (sealed class для type-safe transfer)

```kotlin
sealed class AddUserData {
    data class ById(val telegramId: Long) : AddUserData()
    data class ByUsername(val username: String) : AddUserData()
}
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
│   ├── BotUserEntity.kt
│   ├── BotUserInvitation.kt
│   └── AddUserData.kt
├── repository/
│   ├── BotUserRepository.kt
│   └── BotUserInvitationRepository.kt
├── service/
│   ├── BotUserService.kt
│   ├── InvitationService.kt
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

### 4.2 Repositories

```kotlin
@Repository
interface BotUserRepository : JpaRepository<BotUserEntity, Long> {
    fun findByTelegramId(telegramId: Long): BotUserEntity?
    fun findByUsernameIgnoreCase(username: String): BotUserEntity?
    fun existsByTelegramId(telegramId: Long): Boolean
    fun existsByUsernameIgnoreCase(username: String): Boolean

    @Modifying
    @Transactional
    fun deleteByTelegramId(telegramId: Long)
}

@Repository
interface BotUserInvitationRepository : JpaRepository<BotUserInvitation, Long> {
    fun findByUsernameIgnoreCase(username: String): BotUserInvitation?
    fun existsByUsernameIgnoreCase(username: String): Boolean

    @Modifying
    @Transactional
    fun deleteByUsernameIgnoreCase(username: String)

    @Modifying
    @Transactional
    fun deleteById(id: Long)
}
```

### 4.3 AdminProperties

```kotlin
@ConfigurationProperties(prefix = "notifier.admin")
data class AdminProperties(
    val masterAdminId: Long
)
```

### 4.4 AuthorizationService

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
        return user.role.hasPermission(requiredRole)
    }

    fun getUser(telegramId: Long): BotUserEntity? =
        botUserRepository.findByTelegramId(telegramId)

    fun isMasterAdmin(telegramId: Long): Boolean =
        telegramId == adminProperties.masterAdminId
}
```

### 4.5 InvitationService

```kotlin
@Service
@Transactional
class InvitationService(
    private val invitationRepository: BotUserInvitationRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun create(username: String, role: UserRole, createdBy: Long): BotUserInvitation {
        val invitation = invitationRepository.save(
            BotUserInvitation(
                username = username.lowercase(),
                role = role,
                createdBy = createdBy
            )
        )
        logger.info("Invitation created: @$username, role=$role, by=$createdBy")
        return invitation
    }

    fun findByUsername(username: String): BotUserInvitation? =
        invitationRepository.findByUsernameIgnoreCase(username.lowercase())

    fun existsByUsername(username: String): Boolean =
        invitationRepository.existsByUsernameIgnoreCase(username.lowercase())

    fun delete(username: String) {
        invitationRepository.deleteByUsernameIgnoreCase(username.lowercase())
        logger.info("Invitation deleted: @$username")
    }

    fun deleteById(id: Long) {
        invitationRepository.deleteById(id)
        logger.info("Invitation deleted: id=$id")
    }

    fun getAll(): List<BotUserInvitation> =
        invitationRepository.findAll()
}
```

### 4.6 BotUserService

```kotlin
@Service
@Transactional
class BotUserService(
    private val botUserRepository: BotUserRepository,
    private val invitationService: InvitationService,
    private val botCommandMenuService: BotCommandMenuService,
    private val adminProperties: AdminProperties,
    private val telegramBot: TelegramBot
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getAllUsers(): List<BotUserEntity> =
        botUserRepository.findAll()
            .filter { it.telegramId != adminProperties.masterAdminId }

    fun existsByTelegramId(telegramId: Long): Boolean =
        botUserRepository.existsByTelegramId(telegramId)

    fun existsByUsername(username: String): Boolean =
        botUserRepository.existsByUsernameIgnoreCase(username.lowercase())

    /**
     * Create user by telegramId (direct add)
     */
    fun create(
        telegramId: Long,
        role: UserRole,
        username: String? = null,
        firstName: String? = null,
        lastName: String? = null
    ): BotUserEntity {
        val user = botUserRepository.save(
            BotUserEntity(
                telegramId = telegramId,
                role = role,
                username = username,
                firstName = firstName,
                lastName = lastName
            )
        )

        runBlocking { botCommandMenuService.updateCommandsForUser(telegramId, role) }
        notifyUserAboutAccess(telegramId)

        logger.info("User created: telegramId=$telegramId, role=$role")
        return user
    }

    /**
     * Activate user from invitation (on /start)
     */
    fun activateFromInvitation(
        invitation: BotUserInvitation,
        telegramId: Long,
        firstName: String?,
        lastName: String?
    ): BotUserEntity {
        val user = botUserRepository.save(
            BotUserEntity(
                telegramId = telegramId,
                role = invitation.role,
                username = invitation.username,
                firstName = firstName,
                lastName = lastName
            )
        )

        invitationService.delete(invitation.username)
        runBlocking { botCommandMenuService.updateCommandsForUser(telegramId, invitation.role) }

        logger.info("User activated from invitation: @${invitation.username}, telegramId=$telegramId")
        return user
    }

    fun delete(telegramId: Long) {
        botUserRepository.deleteByTelegramId(telegramId)
        runBlocking { botCommandMenuService.resetCommandsForUser(telegramId) }
        logger.info("User deleted: telegramId=$telegramId")
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

    /**
     * Cleanup orphaned invitation if user already registered with this username
     */
    fun cleanupOrphanedInvitation(username: String?) {
        if (username != null && invitationService.existsByUsername(username)) {
            invitationService.delete(username)
            logger.info("Cleaned up orphaned invitation: @$username")
        }
    }

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
    GET_USERNAME("get_username", UserRole.ADMIN),
    SELECT_ROLE("select_role", UserRole.ADMIN);
}
```

### 4.10 SilentChatException + Handler

```kotlin
class SilentChatException : ChatException("")

@Component
class SilentExceptionHandler : ExceptionHandler({
    throwable<SilentChatException> {
        // Silent - no response
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
            telegramBot.setMyCommands(
                commands = BotCommands.forRole(role).map { it.toBotCommand() },
                scope = BotCommandScopeChat(chatId = telegramId.toString())
            )
            logger.debug("Updated commands for $telegramId")
        } catch (e: Exception) {
            logger.warn("Failed to update commands for $telegramId: ${e.message}")
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

        // Master admin
        telegramBot.setMyCommands(
            commands = BotCommands.all(),
            scope = BotCommandScopeChat(chatId = adminProperties.masterAdminId.toString())
        )

        // Each registered user
        botUserRepository.findAll().forEach { user ->
            updateCommandsForUser(user.telegramId, user.role)
        }

        logger.info("Bot commands initialized")
    }
}
```

---

## 5. Handlers

### 5.1 StartHandler

```kotlin
@HandlerComponent
class StartHandler(
    private val botUserService: BotUserService,
    private val invitationService: InvitationService,
    private val authorizationService: AuthorizationService
) : BotHandler({

    command("/start") {
        val telegramId = from.id
        val username = from.username

        // Already registered?
        val existingUser = authorizationService.getUser(telegramId)
        if (existingUser != null) {
            botUserService.updateUserInfo(telegramId, username, from.firstName, from.lastName)
            // Cleanup orphaned invitation if username changed to one with pending invitation
            botUserService.cleanupOrphanedInvitation(username)
            sendMessage("С возвращением! /help для списка команд.")
            return@command
        }

        // Has invitation?
        if (username != null) {
            val invitation = invitationService.findByUsername(username)
            if (invitation != null) {
                botUserService.activateFromInvitation(
                    invitation = invitation,
                    telegramId = telegramId,
                    firstName = from.firstName,
                    lastName = from.lastName
                )
                sendMessage("Добро пожаловать! /help для списка команд.")
                return@command
            }
        }

        // No access
        sendMessage("Нет доступа. Обратись к администратору.")
    }
})
```

### 5.2 ListUsersHandler

```kotlin
@HandlerComponent
class ListUsersHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val invitationService: InvitationService
) : SecureBotHandler(auth, {

    command(BotCommands.LIST_USERS) {
        val users = botUserService.getAllUsers()
        val invitations = invitationService.getAll()

        val text = buildString {
            // Registered users
            appendLine("📋 Пользователи (${users.size}):")
            if (users.isEmpty()) {
                appendLine("  — пусто —")
            } else {
                users.forEachIndexed { i, user ->
                    val name = user.username?.let { "@$it" } ?: user.telegramId.toString()
                    appendLine("${i + 1}. $name")
                    appendLine("   Роль: ${user.role.name.lowercase()}")
                    appendLine("   ID: ${user.telegramId}")
                }
            }

            // Pending invitations
            if (invitations.isNotEmpty()) {
                appendLine()
                appendLine("⏳ Ожидают активации (${invitations.size}):")
                invitations.forEachIndexed { i, inv ->
                    appendLine("${i + 1}. @${inv.username} (${inv.role.name.lowercase()})")
                }
            }
        }

        // Show with cancel buttons if there are invitations
        if (invitations.isNotEmpty()) {
            sendMessage(
                text,
                replyMarkup = inlineKeyboard(
                    *invitations.map { inv ->
                        callbackButton("❌ @${inv.username}", "admin:cancel_inv:${inv.id}")
                    }.toTypedArray()
                )
            )
        } else {
            sendMessage(text)
        }
    }

    // Cancel invitation
    callbackRegex(Regex("admin:cancel_inv:(\\d+)"), UserRole.ADMIN) { match ->
        val invitationId = match.groupValues[1].toLong()
        invitationService.deleteById(invitationId)
        sendMessage("✅ Приглашение отменено")
    }
})
```

### 5.3 AddUserHandler

```kotlin
@HandlerComponent
class AddUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val invitationService: InvitationService
) : SecureBotHandler(auth, {

    command(BotCommands.ADD_USER) {
        sendMessage(
            "Как добавить пользователя?",
            replyMarkup = inlineKeyboard(
                callbackButton("По Telegram ID", BotCallbacks.ADMIN_ADD_BY_ID.callback),
                callbackButton("По @username", BotCallbacks.ADMIN_ADD_BY_USERNAME.callback),
                callbackButton("Отмена", BotCallbacks.ADMIN_CANCEL.callback)
            )
        )
    }

    // === By Telegram ID ===

    callback(BotCallbacks.ADMIN_ADD_BY_ID, next = BotSteps.GET_USER_ID.step) {
        sendMessage("Введи Telegram ID:")
    }

    step(BotSteps.GET_USER_ID) {  // без next — после этого inline keyboard
        val telegramId = text.toLongOrNull()?.takeIf { it > 0 }
            ?: throw ChatException("Некорректный ID. Введи положительное число.")

        if (botUserService.existsByTelegramId(telegramId)) {
            throw ChatException("Пользователь с ID $telegramId уже существует")
        }

        transfer(AddUserData.ById(telegramId))
        sendMessage(
            "Выбери роль:",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    // === By Username ===

    callback(BotCallbacks.ADMIN_ADD_BY_USERNAME, next = BotSteps.GET_USERNAME.step) {
        sendMessage("Введи @username:")
    }

    step(BotSteps.GET_USERNAME) {  // без next — после этого inline keyboard
        val username = text.removePrefix("@").lowercase()

        if (!username.matches(Regex("^[a-z0-9_]{5,32}$"))) {
            throw ChatException("Некорректный username (5-32 символа: a-z, 0-9, _)")
        }

        if (botUserService.existsByUsername(username)) {
            throw ChatException("Пользователь @$username уже зарегистрирован")
        }

        if (invitationService.existsByUsername(username)) {
            throw ChatException("Приглашение для @$username уже существует")
        }

        transfer(AddUserData.ByUsername(username))
        sendMessage(
            "Выбери роль:",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    // === Fallback step if user types text instead of clicking button ===

    step(BotSteps.SELECT_ROLE) {
        sendMessage(
            "Нажми кнопку выше ☝️",
            replyMarkup = inlineKeyboard(
                callbackButton("User", BotCallbacks.ROLE_USER.callback),
                callbackButton("Admin", BotCallbacks.ROLE_ADMIN.callback)
            )
        )
    }

    // === Role selection callbacks ===

    callback(BotCallbacks.ROLE_USER) {
        when (val data = transferred<AddUserData>()) {
            is AddUserData.ById -> {
                botUserService.create(data.telegramId, UserRole.USER)
                sendMessage("✅ Пользователь добавлен (USER)")
            }
            is AddUserData.ByUsername -> {
                invitationService.create(data.username, UserRole.USER, from.id)
                sendMessage("✅ Приглашение создано для @${data.username} (USER)")
            }
        }
    }

    callback(BotCallbacks.ROLE_ADMIN) {
        when (val data = transferred<AddUserData>()) {
            is AddUserData.ById -> {
                botUserService.create(data.telegramId, UserRole.ADMIN)
                sendMessage("✅ Пользователь добавлен (ADMIN)")
            }
            is AddUserData.ByUsername -> {
                invitationService.create(data.username, UserRole.ADMIN, from.id)
                sendMessage("✅ Приглашение создано для @${data.username} (ADMIN)")
            }
        }
    }

    callback(BotCallbacks.ADMIN_CANCEL) {
        sendMessage("Отменено")
    }
})
```

### 5.4 RemoveUserHandler

```kotlin
@HandlerComponent
class RemoveUserHandler(
    auth: AuthorizationService,
    private val botUserService: BotUserService,
    private val adminProperties: AdminProperties
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

        if (telegramId == adminProperties.masterAdminId) {
            sendMessage("❌ Нельзя удалить master admin")
            return@callbackRegex
        }

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

        if (telegramId == adminProperties.masterAdminId) {
            sendMessage("❌ Нельзя удалить master admin")
            return@callbackRegex
        }

        botUserService.delete(telegramId)
        sendMessage("✅ Удалён")
    }
})
```

---

## 6. Конфигурация

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

## 7. Flow диаграммы

### 7.1 Registration Flow

```
┌─────────────────────────────────────────────────────────────┐
│                   /add_user @username                        │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  bot_user_invitation   │
              │  username = "john"     │
              │  role = USER           │
              │  created_by = 123      │
              └────────────────────────┘
                           │
                           │  @john делает /start
                           ▼
              ┌────────────────────────┐
              │  Найдено invitation?   │
              └────────────────────────┘
                     │          │
                    YES        NO
                     │          │
                     ▼          ▼
              ┌──────────┐  ┌──────────────┐
              │ bot_user │  │"Нет доступа" │
              │ создан   │  └──────────────┘
              └──────────┘
                     │
                     ▼
              ┌──────────────────────┐
              │ invitation удалён    │
              │ меню команд обновлено│
              └──────────────────────┘
```

### 7.2 Authorization Flow

```
┌─────────────────────────────────────────────────────────────┐
│                      Any Command/Callback                    │
└─────────────────────────────────────────────────────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │ SecureBotHandler       │
              │ auth.isAuthorized()    │
              └────────────────────────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
    masterAdmin?     requiredRole    user.role >=
         │            == null?      requiredRole?
         ▼               ▼               ▼
        YES             YES             YES/NO
         │               │               │
         └───────────────┴───────┬───────┘
                                 │
                    ┌────────────┴────────────┐
                    ▼                         ▼
              ┌──────────┐            ┌──────────────────┐
              │  ALLOW   │            │      DENY        │
              │  action()│            │SilentChatException│
              └──────────┘            └──────────────────┘
```

---

## 8. Безопасность

### Реализовано
- Master admin через env
- Enum-based авторизация (compile-time safety)
- Sealed class для type-safe transfer
- telegramId всегда NOT NULL
- Invitation удаляется после использования
- Защита от удаления master admin (в handler)
- Валидация в handlers (ChatException), не в сервисах
- Cleanup orphaned invitations
- Fallback step для text вместо callback
- @Transactional + @Modifying

### Закрытые векторы

| Вектор | Защита |
|--------|--------|
| Повторное использование invitation | Удаляется при активации |
| telegramId = 0 hack | Отдельная таблица invitations |
| UNIQUE constraint violation | Проверки в handlers |
| Unsafe casts | Sealed class AddUserData |
| Text вместо callback | Fallback step SELECT_ROLE |
| Orphaned invitations | Cleanup в StartHandler |
| Удаление master admin | Проверка в RemoveUserHandler |

---

## 9. TODO

- [ ] Audit log
- [ ] Rate limiting
- [ ] Пагинация в /list_users (если много пользователей)
- [ ] Срок действия invitation (expiration)
- [ ] Изменение роли существующего пользователя
