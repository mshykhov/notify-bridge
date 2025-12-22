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
    implementation("com.smhomelab:notifier-api:0.1.0")
}
```

## Configuration

Add to `application.yml`:

```yaml
smhomelab:
  client:
    enabled: true
    base-url: https://notifier.your-domain.com
    connect-timeout: 10s
    read-timeout: 30s

    oauth2:
      enabled: true
      token-uri: https://your-auth0-domain.auth0.com/oauth/token
      client-id: your-client-id
      client-secret: your-client-secret
      audience: https://notifier.your-domain.com
      scopes:
        - notifications:send

    retry:
      enabled: true
      max-attempts: 3
      base-delay: 500ms
      multiplier: 2.0
```

### Properties Reference

| Property | Default | Description |
|----------|---------|-------------|
| `smhomelab.client.enabled` | `true` | Enable/disable client |
| `smhomelab.client.base-url` | - | Notifier service URL |
| `smhomelab.client.connect-timeout` | `10s` | Connection timeout |
| `smhomelab.client.read-timeout` | `30s` | Read timeout |
| `smhomelab.client.oauth2.enabled` | `true` | Enable OAuth2 auth |
| `smhomelab.client.oauth2.token-uri` | - | OAuth2 token endpoint |
| `smhomelab.client.oauth2.client-id` | - | OAuth2 client ID |
| `smhomelab.client.oauth2.client-secret` | - | OAuth2 client secret |
| `smhomelab.client.oauth2.audience` | - | OAuth2 audience |
| `smhomelab.client.retry.enabled` | `true` | Enable retry |
| `smhomelab.client.retry.max-attempts` | `3` | Max retry attempts |

## Usage

```kotlin
@Service
class MyService(
    private val notifierClient: NotifierClient
) {
    suspend fun sendNotification() {
        // Send Telegram notification
        val response = notifierClient.sendTelegram(
            TelegramNotificationRequest(
                message = "Hello from my service!",
                parseMode = "HTML",
                disableNotification = false
            )
        )

        // Send Pushover notification
        val pushoverResponse = notifierClient.sendPushover(
            PushoverNotificationRequest(
                message = "Important alert!",
                title = "Alert",
                priority = NotificationPriority.HIGH,
                sound = PushoverSound.SIREN
            )
        )

        // Get rate limits
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
