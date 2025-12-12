# Детальный отчёт: Админ-панель Notifier Service

## 1. Архитектурный обзор

### 1.1 Ключевое открытие
**Админ-панель реализована НЕ как веб-интерфейс, а как Telegram Bot.** Это нестандартный подход, где бот одновременно является:
- Пользовательским интерфейсом для обычных пользователей
- Админ-панелью для администраторов

### 1.2 Технологический стек
| Компонент | Технология |
|-----------|------------|
| Framework | Spring Boot 3.2.4 + WebFlux (реактивный) |
| Язык | Kotlin 2.1.0 |
| Telegram API | `org.telegram:telegrambots:6.9.7.1` |
| База данных | PostgreSQL + JPA/Hibernate |
| Messaging | Apache Kafka |
| Service Discovery | Eureka (отключен) |

---

## 2. Модель авторизации

### 2.1 Роли пользователей
```kotlin
// UserRole.kt:3-6
enum class UserRole {
    USER,   // Обычный пользователь
    ADMIN,  // Администратор
}
```

### 2.2 Иерархия доступа
```
Master Admin (env: MG_NOTIFIER_ADMIN_CHAT_ID)
     ↓ (все права автоматически)
   ADMIN роль
     ↓ (включает USER права)
   USER роль
     ↓
Неавторизованные (только /start)
```

### 2.3 Механизм проверки прав
Файл: `PollingCommand.kt:26-42`

```kotlin
final fun isUserPermitted(
    masterAdminId: Long,
    requestChatId: Long?,
    userChatId: Long?,
    userRoles: Set<UserRole>,
): Boolean {
    // Master Admin всегда имеет доступ
    if (masterAdminId == requestChatId) return true
    // Команды без требований доступны всем
    if (requiredRoles.isEmpty()) return true
    // Нет chatId = не авторизован
    if (userChatId == null) return false
    // Проверка наличия всех необходимых ролей
    return userRoles.containsAll(requiredRoles)
}
```

### 2.4 Master Admin
Файл: `UserDataService.kt:20-38`

При старте приложения автоматически создаётся/обновляется Master Admin:
```kotlin
@PostConstruct
fun addMasterAdminUser() {
    val existing = userDataRepoWrapper.findByChatId(telegramProperties.bot.masterAdminId)
    if (existing == null) {
        // Создание с ВСЕМИ ролями
        userDataRepoWrapper.save(
            UserDataEntity(
                chatId = telegramProperties.bot.masterAdminId,
                username = "admin",
                roles = UserRole.entries.toMutableSet(), // USER + ADMIN
            ),
        )
    } else if (!existing.roles.containsAll(UserRole.entries)) {
        // Восстановление всех ролей если потеряны
        existing.roles.clear()
        existing.roles.addAll(UserRole.entries)
        userDataRepoWrapper.save(existing)
    }
}
```

---

## 3. Административные команды

### 3.1 Полный список команд
Файл: `PollingCommand.kt:7-22`

| Команда | Требуемая роль | Описание |
|---------|---------------|----------|
| `/start` | — | Начать работу с ботом |
| `/help` | USER | Показать справку |
| `/opportunities` | USER | Список пар пользователя |
| `/add_opportunity` | USER | Добавить пару для мониторинга |
| `/set_time_zone` | USER | Установить тайм зону |
| `/get_time_zone` | USER | Показать тайм зону |
| `/exchanges` | USER | Список бирж |
| **`/list_users`** | **ADMIN** | Список пользователей |
| **`/add_user`** | **ADMIN** | Добавить пользователя |
| **`/remove_user`** | **ADMIN** | Удалить пользователя |

### 3.2 Детальный анализ админ-команд

#### `/list_users` - Просмотр пользователей
Файл: `ListUserPollingCommandHandler.kt:19-74`

**Функционал:**
- Получает всех пользователей кроме Master Admin
- Для каждого пользователя отображает:
  - Username с эмодзи статуса (✅ активен / ⚠️ не активирован)
  - Роли (user, admin или "без ролей")
  - Список Opportunities с критериями и уведомлениями
  - ChatId (если есть)
  - ShortId (технический идентификатор)

**Формат вывода (HTML):**
```
📋 Список пользователей (N):

1. @username ✅
   • Роли: user
   • Opportunities:
     1) BTC/USDT (n - 3)(fn - 2)
   • ChatId: 123456789
   • ID: ud_xxxxxxxxxx
```

#### `/add_user <@username>` - Добавление пользователя
Файл: `AddUserPollingCommandHandler.kt:13-45`

**Функционал:**
- Валидация username: `^@[a-zA-Z0-9_]{5,32}$`
- Создание пользователя с ролью `USER`
- Без chatId (устанавливается при первом /start)

**Процесс:**
```
Admin: /add_user @newuser
       ↓
Валидация regex → Создание UserDataEntity (chatId=null, role=USER)
       ↓
Bot: ✅ Пользователь @newuser добавлен
```

#### `/remove_user <@username>` - Удаление пользователя
Файл: `RemoveUserPollingCommandHandler.kt:13-45`

**Функционал:**
- Валидация username
- Удаление пользователя из БД
- Очистка команд бота для этого пользователя

**Процесс:**
```
Admin: /remove_user @olduser
       ↓
userData.removeUser() → commandService.removeBotCommandsForUser()
       ↓
Bot: ✅ Пользователь @olduser удален
```

---

## 4. Обработка команд

### 4.1 Главный контроллер
Файл: `TelegramPollingBotController.kt:30-160`

**Архитектура:**
```
Update (Telegram)
     ↓
TelegramPollingBotController.onUpdateReceived()
     ↓
┌─────────────────────────────────────────┐
│ message.startsWith("/") ?               │
│   → handleCommand() → PollingCommandHandler
│                                         │
│ hasCallbackQuery() ?                    │
│   → handleCallback() → CallbackCommandHandler
│                                         │
│ else (text message) ?                   │
│   → handleTextMessage() → InputCommandHandler
└─────────────────────────────────────────┘
```

### 4.2 Валидация параметров
Файл: `PollingCommandHandler.kt:76-101`

```kotlin
protected fun validate(update: Update): Map<String, String?> {
    val args = getArguments(update)
    val errors = mutableMapOf<String, String?>()

    // Проверка количества аргументов
    val requiredCount = parameters.count { it.isRequired }
    if (args.size < requiredCount) {
        errors["_general"] = "Недостаточно аргументов..."
        return errors
    }

    // Валидация каждого параметра
    parameters.forEachIndexed { index, param ->
        if (index < args.size) {
            if (!param.validator(args[index])) {
                errors[param.name] = param.errorMessage
            }
        } else if (param.isRequired) {
            errors[param.name] = "Обязательный параметр отсутствует"
        }
    }
    return errors
}
```

### 4.3 Проверка авторизации при обработке команды
Файл: `PollingCommandHandler.kt:109-118`

```kotlin
final suspend fun processCommand(bot: TelegramLongPollingBot, update: Update) {
    if (!command.isUserPermitted(
            telegramProperties.bot.masterAdminId,
            update.message.chatId,
            userData.getByChatId(update.message.chatId, update.message.chat.userName),
        )
    ) {
        logger.debug("Пользователь ... попытался выполнить команду ... без необходимых прав")
        return  // Молчаливый отказ - никакого сообщения пользователю
    }
    // ... выполнение команды
}
```

---

## 5. Управление командами в меню Telegram

### 5.1 Динамическое меню команд
Файл: `CommandService.kt:22-54`

**Стратегия:**
1. **Для всех** - команды без требований к ролям
2. **Для Master Admin** - ВСЕ команды
3. **Для каждого активного пользователя** - команды по его ролям

```kotlin
suspend fun updateBotCommands(bot: TelegramLongPollingBot) {
    // 1. Глобальные команды (для незарегистрированных)
    bot.execute(SetMyCommands().apply {
        commands = getCommandsForEveryone()  // только /start
    })

    // 2. Команды для Master Admin
    bot.execute(SetMyCommands().apply {
        commands = PollingCommand.entries.map { it.command }  // ВСЕ
        scope = BotCommandScopeChat().apply { chatId = masterAdminId.toString() }
    })

    // 3. Персональные команды для каждого пользователя
    userData.getActiveUsers().forEach { user ->
        bot.execute(SetMyCommands().apply {
            commands = PollingCommand.entries
                .filter { it.isUserPermitted(..., user) }
                .map { it.command }
            scope = BotCommandScopeChat().apply { chatId = user.chatId.toString() }
        })
    }
}
```

### 5.2 Очистка команд при удалении пользователя
Файл: `CommandService.kt:69-78`

```kotlin
suspend fun removeBotCommandsForUser(bot: TelegramLongPollingBot, userChatId: Long) {
    bot.execute(SetMyCommands().apply {
        commands = getCommandsForEveryone()  // Сброс до базовых команд
        scope = BotCommandScopeChat().apply { chatId = userChatId.toString() }
    })
}
```

---

## 6. Модель данных

### 6.1 UserDataEntity
Файл: `UserDataEntity.kt:27-60`

```kotlin
@Entity
@Table(name = "user_data")
class UserDataEntity(
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(unique = true, length = 13, nullable = false)
    val shortId: String = IdGenerator.generateStructured(13, "ud_"),

    @Column(unique = true)
    val chatId: Long? = null,  // null до первого /start

    @Column(unique = true)
    var username: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    val roles: MutableSet<UserRole> = mutableSetOf(),

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, orphanRemoval = true, cascade = [CascadeType.ALL])
    val userOpportunities: MutableSet<UserOpportunityEntity> = mutableSetOf(),

    @Column
    var timeZone: String = "UTC",
) : JpaAuditable()
```

### 6.2 Схема БД
```
┌─────────────────────────────────────┐
│           user_data                  │
├─────────────────────────────────────┤
│ id          UUID PRIMARY KEY         │
│ short_id    VARCHAR(13) UNIQUE       │
│ chat_id     BIGINT UNIQUE (nullable) │
│ username    VARCHAR UNIQUE           │
│ time_zone   VARCHAR DEFAULT 'UTC'    │
│ created_at  TIMESTAMP (audit)        │
│ updated_at  TIMESTAMP (audit)        │
└─────────────────────────────────────┘
           │
           │ 1:N
           ↓
┌─────────────────────────────────────┐
│          user_roles                  │
├─────────────────────────────────────┤
│ user_id     UUID FK → user_data.id   │
│ roles       VARCHAR (USER/ADMIN)     │
└─────────────────────────────────────┘
```

---

## 7. Lifecycle пользователя

```
1. СОЗДАНИЕ (через /add_user)
   Admin → /add_user @newuser
         ↓
   UserDataEntity(chatId=null, roles=[USER], username="newuser")

2. АКТИВАЦИЯ (при первом /start)
   User → /start в боте
         ↓
   getByChatId() → null
   getUserByUsername() → найден (по username из Telegram)
   assignUserChatId(user, chatId) → обновление chatId
   commandService.updateBotCommands() → появляются команды в меню

3. ИСПОЛЬЗОВАНИЕ
   User → команды согласно ролям
   Admin → админ-команды

4. УДАЛЕНИЕ (через /remove_user)
   Admin → /remove_user @user
         ↓
   userData.removeUser() → DELETE из БД
   commandService.removeBotCommandsForUser() → сброс меню
```

---

## 8. Конфигурация

### 8.1 Переменные окружения для админ-панели
```yaml
# application.yml:95-99
telegram:
  bot:
    username: ${MG_NOTIFIER_BOT_USERNAME}
    token: ${MG_NOTIFIER_BOT_TOKEN}
    masterAdminId: ${MG_NOTIFIER_ADMIN_CHAT_ID}  # ID Telegram чата Master Admin
```

### 8.2 Swagger UI (API документация)
```yaml
# application.yml:7-10
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    enabled: ${MG_SWAGGER_ENABLED:false}  # Отключен по умолчанию
```

---

## 9. Безопасность

### 9.1 Сильные стороны
- Ролевая модель авторизации
- Master Admin не отображается в списке пользователей
- Валидация параметров команд (regex)
- Динамическое меню команд по ролям
- Молчаливый отказ при недостаточных правах

### 9.2 Слабые места / Риски
- Нет аудит-лога административных действий
- Нет подтверждения удаления пользователя
- Master Admin определяется только по chatId (компрометация env → полный доступ)
- Нет rate limiting для админ-команд
- Username можно сменить в Telegram → потенциальный обход

---

## 10. Диаграмма потоков

```
┌─────────────────────────────────────────────────────────────────────┐
│                        TELEGRAM UPDATE                               │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│              TelegramPollingBotController                            │
│   onUpdateReceived() → handleCommand() / handleCallback()            │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│              PollingCommandHandler.processCommand()                  │
│                                                                      │
│  1. command.isUserPermitted(masterAdminId, chatId, user)            │
│     ├─ masterAdminId == chatId? → ALLOW                             │
│     ├─ requiredRoles.isEmpty()? → ALLOW                             │
│     ├─ user.chatId == null? → DENY                                  │
│     └─ userRoles.containsAll(requiredRoles)? → ALLOW/DENY           │
│                                                                      │
│  2. validate(update) → параметры корректны?                         │
│                                                                      │
│  3. execute(bot, update) → выполнение команды                       │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  ↓
┌─────────────────────────────────────────────────────────────────────┐
│    AddUserHandler / RemoveUserHandler / ListUserHandler              │
│                          ↓                                           │
│                   UserDataService                                    │
│                          ↓                                           │
│                  UserDataRepoWrapper                                 │
│                          ↓                                           │
│                     PostgreSQL                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 11. Выводы

1. **Нестандартный подход** - админ-панель через Telegram бот вместо веб-интерфейса
2. **Простая ролевая модель** - всего 2 роли (USER, ADMIN) + Master Admin
3. **3 админ-команды** - минимальный, но достаточный функционал
4. **Двухэтапная регистрация** - сначала admin добавляет username, затем пользователь активирует через /start
5. **Динамическое меню** - каждый пользователь видит только доступные ему команды
6. **Отсутствие веб-UI** - нет HTML/JS/CSS, всё управление через бота
