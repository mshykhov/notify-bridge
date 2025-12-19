# Telegram Rate Limiting

**Commit**: 36871c5

---

## Обзор

Реализован rate limiting для исходящих Telegram сообщений с LRU cleanup.

## Архитектура

```
┌─────────────────────────┐     ┌──────────────────────────┐
│  TelegramMessageService │     │   Bot Handlers           │
│  (proactive messages)   │     │   (response messages)    │
│  ✅ Rate limiting       │     │   Не требует limiting    │
│  - send()               │     │   - sendMessage()        │
│  - globalLimiter        │     │   - editMessageText()    │
│  - chatLimiters (LRU)   │     │                          │
└─────────────────────────┘     └──────────────────────────┘
```

### Почему два пути?

Telegram API различает:
- **Response messages** (ответ на действие юзера) — практически без лимитов
- **Proactive messages** (бот инициирует) — 30/сек global, 1/сек per-chat

Handlers отвечают на команды/callbacks → response messages → не нужен rate limiting.
TelegramMessageService используется для уведомлений → proactive → нужен rate limiting.

## Компоненты

### RateLimiter (Token Bucket)

```kotlin
class RateLimiter(
    tokensPerSecond: Double,
    maxTokens: Int,
)
```

- Алгоритм: Token Bucket с refill
- Thread-safe: Kotlin Mutex
- Suspend-friendly: использует `delay()` вместо блокировки

### ChatRateLimiters (LRU)

```kotlin
class ChatRateLimiters(
    tokensPerSecond: Double,
    maxTokens: Int,
)
```

- Per-chat лимитеры с tracking `lastAccessTime`
- LRU cleanup: удаляет самые старые при превышении maxSize
- Scheduled cleanup: каждый час, max 1000 записей

### TelegramMessageService

```kotlin
@Service
class TelegramMessageService(
    telegramBot: TelegramBot,
    config: RateLimitProperties,
)
```

Методы:
- `send()` — отправка с rate limiting и retry
- `cleanupChatLimiters()` — scheduled cleanup (@Scheduled)

## Конфигурация

```yaml
telegram:
  rate-limit:
    enabled: true
    global:
      requests-per-second: 30
      burst: 5
    per-chat:
      requests-per-second: 1
      burst: 2
    retry:
      max-attempts: 3
      base-delay-ms: 1000
```

## Использование

TelegramMessageService используется в:
- `UserService.notifyUserAboutAccess()` — уведомление нового юзера
- `UserService.notifyInviterAboutActivation()` — уведомление inviter'а
- `PushoverLimitsMonitorService.notifyAdmin()` — алерты о лимитах

## Retry логика

При получении 429 (Too Many Requests):
1. Парсится `retry_after` из ответа
2. Ожидание `retry_after + 100ms`
3. Повтор до `maxAttempts`

## Сопутствующие изменения

- `LimitsMonitorService` → `PushoverLimitsMonitorService` (clarity)
