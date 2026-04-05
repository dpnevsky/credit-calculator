# Архитектура системы "Кредитный калькулятор"

## Оглавление

1. [Обзор системы](#обзор-системы)
2. [Архитектура бэкенда](#архитектура-бэкенда)
3. [Архитектура фронтенда](#архитектура-фронтенда)
4. [Аутентификация и авторизация](#аутентификация-и-авторизация)
5. [Взаимодействие фронтенда и бэкенда](#взаимодействие-фронтенда-и-бэкенда)
6. [Инфраструктура](#инфраструктура)
7. [Жизненный цикл кредитной заявки](#жизненный-цикл-кредитной-заявки)
8. [Оценка production-готовности](#оценка-production-готовности)

---

## Обзор системы

Система "Кредитный калькулятор" — это микросервисное веб-приложение для расчёта кредитных параметров, подачи заявок на кредит, скоринга заёмщиков и генерации кредитных документов.

### Технологический стек

| Компонент | Технология |
|-----------|------------|
| **Фронтенд** | TypeScript, React 19, Vite 7, React Router 7 |
| **API Gateway** | Java 21, Spring Boot 3, Spring Cloud Gateway (WebFlux) |
| **Application Service** | Java 21, Spring Boot 3, Spring Data JPA, Liquibase |
| **Scoring Service** | Java 21, Spring Boot 3, Spring Data JPA, Liquibase |
| **Document Service** | Java 21, Spring Boot 3, Spring Data JPA, Liquibase |
| **Аутентификация** | Keycloak (OAuth2/OIDC), keycloak-js |
| **БД** | PostgreSQL (отдельная на каждый сервис) |
| **Брокер сообщений** | Apache Kafka |
| **Контейнеризация** | Docker (multi-stage), Kubernetes (Kustomize) |
| **Сборка** | Gradle (бэкенд), npm/Vite (фронтенд) |

---

## Архитектура бэкенда

### Микросервисная структура

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              Клиент (браузер)                          │
│                         http://localhost:3000                           │
└───────────────────────────────┬─────────────────────────────────────────┘
                                │
                                ▼
┌───────────────────────────────────────────────────────────────────────┐
│                    API Gateway (:8080)                                 │
│          Spring Cloud Gateway + OAuth2 Resource Server                │
│                                                                       │
│  Маршруты:                                                            │
│    /api/applications/** → Application Service (:8081)                  │
│    /api/documents/**    → Application Service (:8081)                  │
│    /*/actuator/**       → Actuator каждого сервиса                     │
└─────┬────────────────────────┬───────────────────────┬────────────────┘
      │                        │                       │
      ▼                        ▼                       ▼
┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐
│ Application  │    │  Scoring Service │    │ Document Service │
│ Service      │    │    (:8082)       │    │    (:8083)       │
│  (:8081)     │    │                  │    │                  │
│              │◄──►│  REST API        │    │  REST API        │
│ PostgreSQL   │    │                  │    │                  │
│ :5433        │    │  PostgreSQL      │    │  PostgreSQL      │
│              │    │  :5434           │    │  :5435           │
└──────┬───────┘    └──────────────────┘    └────────┬─────────┘
       │                                            │
       │              Apache Kafka (:9092)           │
       └────────────────────┬───────────────────────┘
                            │
                  Асинхронные события:
                  - application.scoring.completed
                  - application.documents.request
```

### Сервисы

#### API Gateway (порт 8080)
- **Назначение**: Единая точка входа для всех API-запросов
- **Технология**: Spring Cloud Gateway (реактивный, WebFlux)
- **Функции**:
  - Маршрутизация запросов к микросервисам
  - CORS-политика (глобальная)
  - OAuth2/JWT-валидация токенов (опционально, через `security.debug-mode`)
  - Добавление correlation ID (`X-Correlation-Id`)
  - Проксирование actuator-эндпоинтов

#### Application Service (порт 8081)
- **Назначение**: Управление кредитными заявками
- **БД**: PostgreSQL (:5433, `application_db`)
- **API-эндпоинты**:
  - `POST /api/applications` — создание заявки
  - `GET /api/applications/{id}` — получение заявки
  - `POST /api/applications/{id}/submit` — подача на скоринг
  - `GET /api/applications/{id}/scoring-result` — результат скоринга
  - `GET /api/applications/{id}/offers` — список предложений
  - `POST /api/applications/{id}/offers/{offerId}/select` — выбор предложения
  - `POST /api/applications/{id}/documents/request` — запрос документов
  - `GET /api/documents/{id}/download` — скачивание документа
- **Интеграции**: HTTP-вызовы к Scoring Service и Document Service, Kafka-продюсер

#### Scoring Service (порт 8082)
- **Назначение**: Оценка кредитоспособности заёмщика
- **БД**: PostgreSQL (:5434, `scoring_db`)
- **Функции**: Расчёт кредитного скоринга, генерация предварительных офферов

#### Document Service (порт 8083)
- **Назначение**: Генерация и хранение кредитных документов
- **БД**: PostgreSQL (:5435, `document_db`)
- **Функции**: Создание документов (PDF), Kafka-консьюмер для событий

### Миграции базы данных

Все сервисы используют **Liquibase** для управления схемой БД:
- `db.changelog-master.yaml` — корневой changelog
- Миграции нумеруются: `001-create-tables.sql`, `002-...`, и т.д.
- JPA-валидация: `ddl-auto: validate` — Hibernate проверяет схему, но не меняет её

---

## Архитектура фронтенда

### Структура проекта

```
frontend/
├── public/
│   └── silent-check-sso.html    # Keycloak SSO silent refresh
├── src/
│   ├── components/
│   │   ├── LoanForm.tsx         # Форма калькулятора
│   │   ├── Result.tsx           # Результат расчёта
│   │   ├── Navbar.tsx           # Навигация (auth-зависимая)
│   │   ├── PrivateRoute.tsx     # Защита маршрутов
│   │   └── SubmitApplicationModal.tsx  # Модальная форма подачи
│   ├── context/
│   │   ├── AuthContext.tsx      # Контекст аутентификации
│   │   └── AuthProvider.tsx     # Провайдер (Keycloak init)
│   ├── pages/
│   │   ├── Login.tsx            # Редирект на Keycloak login
│   │   ├── Register.tsx         # Редирект на Keycloak register
│   │   ├── Profile.tsx          # Профиль пользователя
│   │   ├── CreateApplication.tsx # Создание заявки
│   │   ├── ApplicationsList.tsx  # Список заявок
│   │   └── ApplicationDetails.tsx # Детали заявки
│   ├── services/
│   │   ├── auth.service.ts      # Keycloak OAuth2/OIDC клиент
│   │   └── api.service.ts       # Axios API клиент с JWT
│   ├── types/
│   │   └── api.ts               # TypeScript типы API
│   └── App.tsx                  # Маршрутизация + PrivateRoute
└── vite.config.ts               # Proxy: /api→:8080, /auth→:8180
```

### Маршрутизация

| Маршрут | Компонент | Доступ |
|---------|-----------|--------|
| `/` | CalculatorPage | Публичный |
| `/login` | Login | Публичный (редирект на Keycloak) |
| `/register` | Register | Публичный (редирект на Keycloak) |
| `/applications` | ApplicationsList | Только авторизованные |
| `/applications/new` | CreateApplication | Только авторизованные |
| `/applications/:id` | ApplicationDetails | Только авторизованные |
| `/profile` | Profile | Только авторизованные |

### Логика UI

1. **Без авторизации**: пользователь видит только калькулятор и кнопку "Войти"
2. **После входа**: появляются ссылки "Мои заявки", "Оформить кредит", "Профиль", "Выйти"
3. **Защита маршрутов**: компонент `PrivateRoute` проверяет `isAuthenticated` — если нет, редиректит на `/login`

---

## Аутентификация и авторизация

### Схема аутентификации (OAuth2/OIDC + Keycloak)

```
┌──────────┐        ┌──────────────┐        ┌──────────────────────┐
│ Браузер  │        │  Keycloak    │        │   API Gateway        │
│ (React)  │        │  (:8180)     │        │   (:8080)            │
└────┬─────┘        └──────┬───────┘        └──────────┬───────────┘
     │                     │                           │
     │ 1. Инициализация    │                           │
     │    keycloak.init()  │                           │
     │    (check-sso)      │                           │
     │────────────────────►│                           │
     │                     │                           │
     │ 2. Если не авторизован,                         │
     │    нажимает "Войти" │                           │
     │────────────────────►│                           │
     │                     │                           │
     │ 3. Keycloak показывает                          │
     │    форму входа      │                           │
     │◄────────────────────│                           │
     │                     │                           │
     │ 4. Ввод логина/пароля                           │
     │────────────────────►│                           │
     │                     │                           │
     │ 5. Authorization Code                           │
     │    + PKCE exchange  │                           │
     │◄───────────────────►│                           │
     │                     │                           │
     │ 6. Получение JWT    │                           │
     │    (access_token,   │                           │
     │     refresh_token)  │                           │
     │◄────────────────────│                           │
     │                     │                           │
     │ 7. API-запрос с     │                           │
     │    Bearer token     │                           │
     │─────────────────────────────────────────────────►│
     │                     │                           │
     │                     │ 8. Валидация JWT           │
     │                     │◄──────────────────────────│
     │                     │ (проверка подписи,         │
     │                     │  issuer, expiration)       │
     │                     │──────────────────────────►│
     │                     │                           │
     │ 9. Ответ API        │                           │
     │◄─────────────────────────────────────────────────│
```

### Детали реализации

#### Фронтенд (keycloak-js)

```typescript
// Инициализация
keycloak.init({
  onLoad: 'check-sso',              // Проверка SSO без редиректа
  silentCheckSsoRedirectUri: '/silent-check-sso.html',
  checkLoginIframe: false,
  pkceMethod: 'S256',               // PKCE для безопасности public client
});

// Автоматический refresh токена перед каждым API-запросом
api.interceptors.request.use(async (config) => {
  await AuthService.refreshToken(30); // Обновить если < 30 сек до истечения
  const token = AuthService.getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});
```

#### Бэкенд (Spring Security OAuth2 Resource Server)

- **API Gateway**: Валидирует JWT-токены, используя JWK-набор Keycloak
- **Конфигурация**:
  - `issuer-uri`: `http://localhost:8180/realms/credit-calculator`
  - `jwk-set-uri`: `http://localhost:8180/realms/credit-calculator/protocol/openid-connect/certs`
- **Debug Mode**: Переключатель `security.debug-mode=true` отключает проверку JWT (для разработки)

#### Keycloak

- **Realm**: `credit-calculator`
- **Client**: `credit-calculator-app` (public client, PKCE)
- **Тестовые пользователи**:
  - `test-applicant` / `test-password` — роль заёмщика
  - `test-manager` / `test-password` — роль менеджера
- **Роли**: `applicant`, `manager` (realm-level)

### Модель авторизации

| Действие | Требуемая роль | Проверка |
|----------|---------------|----------|
| Калькулятор | Нет (публичный) | — |
| Создание заявки | Авторизованный | JWT в Gateway |
| Просмотр заявок | Авторизованный | JWT в Gateway |
| Скоринг | Авторизованный | JWT в Gateway |
| Документы | Авторизованный | JWT в Gateway |

---

## Взаимодействие фронтенда и бэкенда

### Цепочка запроса

```
React App (localhost:3000)
    │
    │  POST /api/applications  (with Bearer token)
    │
    ▼
Vite Dev Proxy
    │
    │  proxy: /api → http://localhost:8080
    │
    ▼
API Gateway (localhost:8080)
    │
    │  1. Валидация JWT (если debug-mode=false)
    │  2. Добавление X-Correlation-Id
    │  3. Маршрутизация по Path
    │
    ▼
Application Service (localhost:8081)
    │
    │  Бизнес-логика + PostgreSQL
    │
    ▼
JSON Response → API Gateway → Vite Proxy → React App
```

### Формат API-запросов

```http
POST /api/applications HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...

{
  "amount": 500000,
  "term": 24,
  "firstName": "Иван",
  "lastName": "Иванов",
  "email": "ivan@example.com",
  "birthDate": "1990-01-15",
  "passportSeries": "1234",
  "passportNumber": "567890"
}
```

### Обработка ошибок

- **401 Unauthorized**: Токен истёк или отсутствует → фронтенд перенаправляет на Keycloak
- **403 Forbidden**: Недостаточно прав → отображается сообщение об ошибке
- **500 Internal Server Error**: Ошибка сервера → общее сообщение для пользователя

---

## Инфраструктура

### Docker

Каждый сервис имеет **multi-stage Dockerfile**:
1. **Стадия сборки**: Gradle build с Temurin JDK 21
2. **Стадия запуска**: JRE 21 (slim) — минимальный образ

### Kubernetes (Kustomize)

```
deploy/k8s/
├── base/
│   ├── kustomization.yaml
│   ├── api-gateway/
│   ├── application-service/
│   ├── scoring-service/
│   └── document-service/
└── overlays/
    ├── local/
    ├── dev/
    └── prod/
```

### Окружения

| Окружение | Описание | Security |
|-----------|----------|----------|
| **local** | Локальная разработка | debug-mode=true |
| **dev** | Тестовая среда | debug-mode=false, Keycloak |
| **prod** | Production | debug-mode=false, Keycloak, TLS |

---

## Жизненный цикл кредитной заявки

```
1. СОЗДАНИЕ           2. ПОДАЧА              3. СКОРИНГ
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ POST         │    │ POST         │    │ Scoring      │
│ /applications│───►│ /submit      │───►│ Service      │
│              │    │ (+ доп.      │    │ рассчитывает │
│ Статус:      │    │   данные)    │    │ кредитоспо-  │
│ PREAPPROVAL  │    │              │    │ собность     │
└──────────────┘    └──────────────┘    └──────┬───────┘
                                               │
4. ОФФЕРЫ             5. ВЫБОР              6. ДОКУМЕНТЫ
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ GET          │    │ POST         │    │ POST         │
│ /offers      │◄───│ /offers/{id} │───►│ /documents   │
│              │    │ /select      │    │ /request     │
│ Список       │    │              │    │              │
│ предложений  │    │ Статус:      │    │ Генерация    │
│              │    │ APPROVED     │    │ PDF          │
└──────────────┘    └──────────────┘    └──────────────┘
```

### Статусы заявки

| Статус | Описание |
|--------|----------|
| `PREAPPROVAL` | Создана, ожидает подачи |
| `PENDING` | Подана на скоринг |
| `SCORED` | Скоринг завершён |
| `APPROVED` | Оффер выбран, кредит одобрен |
| `REJECTED` | Отказ по скорингу |
| `DOCUMENTS_REQUESTED` | Запрошены документы |
| `DOCUMENTS_READY` | Документы сгенерированы |

---

## Оценка production-готовности

### Что уже реализовано для production

| Аспект | Статус | Описание |
|--------|--------|----------|
| Микросервисная архитектура | Готово | 4 сервиса с разделением ответственности |
| API Gateway | Готово | Единая точка входа, маршрутизация, CORS |
| OAuth2/OIDC | Готово | Keycloak + PKCE, JWT-валидация |
| Миграции БД | Готово | Liquibase с версионированием |
| Docker | Готово | Multi-stage, JRE 21 slim |
| Kubernetes | Готово | Kustomize overlays (local/dev/prod) |
| Structured logging | Готово | JSON для prod, plain text для local |
| Health checks | Готово | Actuator + Kubernetes probes |
| CORS | Готово | Глобальная конфигурация в Gateway |

### Что требует доработки для production

| Аспект | Приоритет | Рекомендация |
|--------|-----------|--------------|
| **HTTPS/TLS** | Критический | Добавить TLS-терминацию (Ingress или Envoy) |
| **Секреты** | Критический | Вынести пароли БД из application.yml в Kubernetes Secrets или Vault |
| **Rate limiting** | Высокий | Добавить ограничение запросов в Gateway |
| **Service Discovery** | Высокий | Заменить жёстко прописанные URL на Kubernetes DNS или Eureka |
| **Мониторинг** | Высокий | Добавить Prometheus + Grafana для метрик |
| **Трейсинг** | Средний | Добавить Micrometer Tracing + Jaeger/Zipkin |
| **CI/CD** | Высокий | Настроить GitHub Actions или GitLab CI |
| **Тесты** | Высокий | Добавить unit/integration тесты (JUnit, Testcontainers) |
| **API-документация** | Средний | Добавить OpenAPI/Swagger для каждого сервиса |
| **CORS ограничение** | Средний | Заменить `allowedOrigins: "*"` на конкретные домены |
| **Circuit Breaker** | Средний | Добавить Resilience4j для межсервисных вызовов |
| **Кэширование** | Низкий | Redis для частых запросов (офферы, скоринг) |
| **Аудит** | Средний | Логирование действий пользователей |
| **Пагинация** | Низкий | Добавить пагинацию в списковые эндпоинты |

### Критические замечания по архитектуре

1. **Жёстко прописанные URL сервисов** — в application.yml URL типа `http://localhost:8081` работают только в dev-режиме. В production нужно использовать Kubernetes service DNS (`http://application-service:8081`)
2. **Один Gateway обслуживает все маршруты** — для высокой нагрузки рассмотреть BFF (Backend for Frontend) паттерн
3. **Нет retry-логики** — при межсервисных HTTP-вызовах нет повторных попыток и circuit breaker
4. **Kafka без dead-letter queue** — сообщения, которые не удалось обработать, теряются
5. **CORS: `allowedOrigins: "*"`** — в production необходимо указать конкретные домены

---

## Порты и адреса (локальная разработка)

| Сервис | Порт | URL |
|--------|------|-----|
| Фронтенд (Vite) | 3000 | http://localhost:3000 |
| API Gateway | 8080 | http://localhost:8080 |
| Application Service | 8081 | http://localhost:8081 |
| Scoring Service | 8082 | http://localhost:8082 |
| Document Service | 8083 | http://localhost:8083 |
| Keycloak | 8180 | http://localhost:8180 |
| PostgreSQL (application) | 5433 | localhost:5433 |
| PostgreSQL (scoring) | 5434 | localhost:5434 |
| PostgreSQL (document) | 5435 | localhost:5435 |
| Kafka | 9092 | localhost:9092 |
