# smhomelab/notifier

**TL;DR:** Telegram-бот на Kotlin + Spring Boot для управления уведомлениями. Маршрутизирует алерты через Telegram и Pushover (critical alerts). PostgreSQL для персистентности.

> **Стек**: Kotlin 2.1, Spring Boot 3.4, Java 21, PostgreSQL 17

---

## Руководство для AI

### Формат ответов
В начале каждого ответа чётко резюмируйте запрос:

```
Ваш запрос: `{краткое описание запроса}`.
Поэтому я `{что, как и почему выполняю}`.
```

Определите тему текущей сессии в первом абзаце.

### Принципы работы
- **Документация в приоритете**: Все задачи — сначала документация (docs/common/important/dev/plan/psa), затем коммит
- **Коммиты по функциям**: Один коммит на одну завершённую функцию/исправление
- **Пропуск тестов**: Только отметка чекбокса, продолжение реализации
- **Запрет TODO/WORKLOG**: Использовать issues/PR

---

## Быстрый старт

```bash
docker-compose up -d          # PostgreSQL
cp .env.example .env          # Конфигурация (заполнить)
./gradlew bootRun             # Запуск
./gradlew ktlintFormat        # Форматирование
```

- DB DSN: `notifier:password@localhost:5432/notifier`
- Обязательные ENV: см. `.env.example`
- Сначала проверьте README (описание функциональности)

---

## 1. Обзор проекта

### 1.1 Текущий стек
- **Runtime**: Kotlin 2.1 + Spring Boot 3.4 + Java 21
- **Bot**: telegram-bot library (dehuckakpyt) с JPA source
- **Notifications**: Telegram API + Pushover API
- **DB**: PostgreSQL 17 + Flyway миграции

### 1.2 Архитектура

```
Handler → Service → Facade → Repository
   ↓         ↓         ↓          ↓
 Telegram  Business  @Trans    Spring
 events    logic     sync      Data JPA
```

| Слой | Основная ответственность | Характеристики |
|------|--------------------------|----------------|
| **Handler** | Bot events, UI flow, inline keyboards | suspend, знает только Service |
| **Service** | Бизнес-логика, валидация, интеграции | suspend, корутины |
| **Facade** | Транзакционные обёртки | @Transactional, синхронный |
| **Repository** | Доступ к DB | Spring Data JPA |

### 1.3 Структура директорий

```
notifier/
  src/main/kotlin/.../notifier/
    bot/           # Commands, callbacks, steps, security
    config/        # Spring config, properties
    handler/       # Bot event handlers
    model/         # Data models (validation, pushover)
    persistence/   # Entities, repositories, facades
    pushover/      # Pushover API client
    service/       # Business logic (suspend)
    telegram/      # Rate limiting, message service
    util/          # Utilities
  src/main/resources/
    application.yml
    db/migration/  # Flyway migrations (V1~)
  docs/
    common/        # Общая документация (хронологическая)
    important/     # Текущие референсы (повышенные)
    dev/           # Записи разработки
    plan/          # Проектная документация/планы
    psa/           # Анализ решения проблем
  .github/         # CI/CD workflows
```

### 1.4 Коммуникация
- Документация: **русский**
- Код/комментарии/коммиты: **английский**

---

## 2. Принципы разработки

### 2.1 Kotlin/Spring стиль
- **Immutability**: prefer `val`, data classes
- **Sealed classes**: для результатов (ValidationResult, SendResult)
- **Suspend functions**: в Handler и Service слоях
- **Extension functions**: для domain-specific DSL

### 2.2 Security (важно!)
- **Все команды требуют проверки роли** через `secureCommand()`, `secureCallback()`, `secureStep()`
- Master Admin из `MASTER_ADMIN_ID` — нельзя удалить
- Новые пользователи только через invitation flow

```kotlin
// Правильно
secureCommand(BotCommands.SETTINGS) { ... }

// Неправильно — нет проверки роли
command("/settings") { ... }
```

### 2.3 Database
- **Flyway**: миграции в `db/migration/V{N}__*.sql`
- **Hibernate**: `ddl-auto: validate` — только Flyway меняет схему
- Новая миграция = новый файл `V{N+1}__description.sql`

### 2.4 Rate Limiting
- Global + Per-chat лимиты для Telegram API
- Automatic retry при rate limit ошибке

### 2.5 Интеграции
- **Telegram**: polling mode, inline keyboards, multi-step flows
- **Pushover**: приоритеты LOWEST(-2) до EMERGENCY(2), мониторинг лимитов

---

## 3. Система документации

### 3.1 Назначение папок

| Папка | Назначение | Правило именования | Нумерация |
|-------|------------|-------------------|-----------|
| `docs/common/` | Эксперименты/заметки/исследования | `{номер}-{тема}-{коммит}.md` | Независимая (000001~) |
| `docs/important/` | Текущие референсы | То же (копия) | Как в common |
| `docs/dev/` | Записи реализации | `{номер}-dev-{функция}-{коммит}.md` | Независимая (000001~) |
| `docs/plan/` | Проектная документация/планы | `{номер}-PLAN-{тема}-{коммит}.md` | Независимая (000001~) |
| `docs/psa/` | Решение проблем | `{номер}-PSA-{категория}-{тема}-{коммит}.md` | Независимая (000001~) |

### 3.2 Правила нумерации
- Каждая папка имеет **независимую нумерацию** начиная с `000001`
- `docs/important/` при повышении **сохраняет оригинальное имя файла**

---

## 4. Рабочий пайплайн

### 4.1 Новая функция (требует планирования)

```
1. Создать docs/plan/{номер}-PLAN-{тема}-000000.md
2. Git коммит → Отразить хеш в имени файла/содержимом → Staging (включить в следующий коммит)
3. Ревью/утверждение (Draft → Approved)
4. Реализация
5. Создать docs/dev/{номер}-dev-{функция}-000000.md
6. Git коммит → Отразить хеш
7. При необходимости повысить до docs/important/
```

### 4.2 Быстрое исправление
```
1. Исправление кода
2. Запись в docs/dev/ или docs/common/
3. Git коммит → Отразить хеш
```

### 4.3 Воркфлоу хеша коммита

**Цикл**: `Создание документа (000000) → Коммит → Отражение хеша в имени/содержимом → Staging`

```bash
# Пример: После создания docs/common/000001-feature-X-000000.md и коммита (хеш: a1b2c3)
git mv docs/common/000001-feature-X-000000.md docs/common/000001-feature-X-a1b2c3.md
vim docs/common/000001-feature-X-a1b2c3.md  # Добавить Commit: a1b2c3
git add docs/common/000001-feature-X-a1b2c3.md  # Включить в следующий коммит
```

---

## 5. Написание планов/PSA

### 5.1 Планы (docs/plan/)

**Критерии написания** (при выполнении хотя бы одного):
- **Обязательно**: Новая подсистема, изменение архитектуры, изменение схемы DB, новая интеграция
- **Рекомендуется**: Изменение 5+ файлов, изменение ключевой логики, изменение взаимодействия компонентов

**Обязательно включить**:
```markdown
**Статус**: Draft/Approved/Implemented/Deprecated
**Commit**: 000000

## 1. Предыстория/Цель
## 2. Текущее состояние
## 3. Проектирование (Архитектура/Компоненты/DB/API)
## 4. Этапы реализации (TODO чекбоксы)
## 5. Область влияния
## 6. Тестирование
## 7. Риски
```

### 5.2 PSA (docs/psa/)

**Для чего**: Исправление багов, повторяющиеся проблемы, сбои сборки/логов и другой траблшутинг

**Коды категорий**:

**Разработка/сборка**
- `build` - Сбои сборки, ошибки компиляции
- `dep` - Проблемы зависимостей (Gradle, Spring)
- `env` - Переменные окружения, проблемы конфигурации

**Код**
- `code` - Общие баги кода (логические ошибки, NPE)
- `coroutine` - Проблемы корутин, suspend функций
- `leak` - Утечки памяти/ресурсов

**Инфраструктура**
- `docker` - Проблемы Docker/контейнеров
- `spring` - Spring Boot конфигурация/поведение
- `db` - DB запросы, проблемы схемы
- `migration` - Сбои Flyway миграций

**Интеграции**
- `telegram` - Telegram Bot API проблемы
- `pushover` - Pushover API проблемы
- `auth` - Аутентификация, роли
- `ratelimit` - Rate limiting проблемы

**Прочее**
- `perf` - Проблемы производительности, оптимизация
- `test` - Сбои тестов, проблемы окружения

Возможны составные теги (например: `PSA-telegram-ratelimit`)

**Структура документа**:
```markdown
**Категория**: PSA-{категория}
**Commit**: 000000

## 1. Определение проблемы
## 2. Условия возникновения/воспроизведение
## 3. Симптомы/логи
## 4. Анализ причин
## 5. Процесс решения
## 6. Последующие действия
```

---

## 6. Политика кода

### 6.1 Архитектура
- **MVC-подобная структура**: Handler (Controller) → Service (Business) → Facade/Repository (Model)
- Чёткое разделение ответственности между слоями

### 6.2 Принципы
- **DRY**: Не повторять логику, выносить в переиспользуемые компоненты
- **SOLID**: Single responsibility, Open/closed, Liskov substitution, Interface segregation, Dependency inversion
- **KISS**: Простые решения предпочтительнее сложных

### 6.3 Организация кода
- **Один файл = один класс**
- **Все DTO и data models** должны находиться в папке `model/`
