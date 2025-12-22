# notifier-api

Client library for smhomelab Notifier service.

## Installation

### Gradle (Kotlin DSL)

```kotlin
repositories {
    maven {
        url = uri("https://repo.repsy.io/mvn/smhomelub/smhomelab")
    }
}

dependencies {
    implementation("com.smhomelab:notifier-api:0.1.2")
}
```

## Configuration

Add to `application.yml`:

```yaml
smhomelab:
  notifier:
    base-url: ${NOTIFIER_BASE_URL}
    oauth2:
      token-uri: ${AUTH0_TOKEN_URI}
      client-id: ${NOTIFIER_AUTH0_CLIENT_ID}
      client-secret: ${NOTIFIER_AUTH0_CLIENT_SECRET}
      audience: ${NOTIFIER_AUTH0_AUDIENCE}
```

### Auth0 Credentials

Credentials are stored in Doppler (configured via terraform in `infrastructure/terraform/auth0`):

| Doppler Variable | Description |
|------------------|-------------|
| `NOTIFIER_AUTH0_CLIENT_ID` | M2M application client ID |
| `NOTIFIER_AUTH0_CLIENT_SECRET` | M2M application client secret |
| `NOTIFIER_AUTH0_AUDIENCE` | API identifier (`https://api.notifier.com`) |

### Properties Reference

| Property | Default | Description |
|----------|---------|-------------|
| `smhomelab.notifier.enabled` | `true` | Enable/disable client |
| `smhomelab.notifier.base-url` | - | Notifier service URL |
| `smhomelab.notifier.connect-timeout` | `10s` | Connection timeout |
| `smhomelab.notifier.read-timeout` | `30s` | Read timeout |
| `smhomelab.notifier.oauth2.enabled` | `true` | Enable OAuth2 auth |
| `smhomelab.notifier.oauth2.token-uri` | - | Auth0 token endpoint |
| `smhomelab.notifier.oauth2.client-id` | - | M2M client ID |
| `smhomelab.notifier.oauth2.client-secret` | - | M2M client secret |
| `smhomelab.notifier.oauth2.audience` | - | API audience |
| `smhomelab.notifier.oauth2.scopes` | `send:telegram, send:pushover, read:limits` | OAuth2 scopes |
| `smhomelab.notifier.retry.enabled` | `true` | Enable retry |
| `smhomelab.notifier.retry.max-attempts` | `3` | Max retry attempts |
| `smhomelab.notifier.retry.base-delay` | `500ms` | Base delay |
| `smhomelab.notifier.retry.multiplier` | `2.0` | Backoff multiplier |

## Usage

```kotlin
@Service
class MyService(
    private val notifierClient: NotifierClient
) {
    suspend fun sendNotification() {
        val response = notifierClient.sendTelegram(
            TelegramNotificationRequest(
                message = "Hello from my service!",
                parseMode = "HTML",
                disableNotification = false
            )
        )

        val pushoverResponse = notifierClient.sendPushover(
            PushoverNotificationRequest(
                message = "Important alert!",
                title = "Alert",
                priority = NotificationPriority.HIGH,
                sound = PushoverSound.SIREN
            )
        )

        val limits = notifierClient.getLimits()
    }
}
```

## API Reference

### NotifierClient

| Method | Description |
|--------|-------------|
| `sendTelegram(request)` | Send Telegram notification |
| `sendPushover(request)` | Send Pushover notification |
| `getLimits()` | Get rate limit information |

### Models

- `TelegramNotificationRequest` - Telegram message params
- `PushoverNotificationRequest` - Pushover message with priority/sound
- `NotificationResponse` - Response with message ID
- `LimitsResponse` - Rate limit info
