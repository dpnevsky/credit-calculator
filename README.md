# Кредитный калькулятор

Практический проект на Java и React: от предварительного расчёта кредита до заявки, скоринга, выбора предложения и формирования PDF-договора. Проект используется для изучения взаимодействия сервисов, авторизации через Keycloak, событий Kafka и разработки с помощью ИИ-ассистентов.

## Возможности

- Расчёт аннуитетных и дифференцированных платежей без регистрации.
- Регистрация, вход, обновление сессии и просмотр профиля.
- Создание заявки и предварительная проверка возраста, суммы и срока.
- Скоринг по данным о занятости, доходе и стаже.
- Выбор предложения с учётом страховки, зарплатного клиента и типа платежей.
- Асинхронное создание PDF-договора, его скачивание и учебное подтверждение подписания.
- Просмотр своих заявок и их текущего состояния.

Правила скоринга и условия кредита условные. Подписание моделируется изменением состояния заявки и идентификатором подтверждения; интеграции с электронной подписью нет.

## Устройство проекта

| Компонент | Назначение | Порт |
| --- | --- | --- |
| `frontend` | React, TypeScript, Vite: калькулятор и личный кабинет | 3000 |
| `api-gateway` | Spring Cloud Gateway: маршрутизация, проверка JWT, API авторизации | 8080 |
| `application-service` | Заявки, предложения, состояния договора, проверка владельца | 8081 |
| `scoring-service` | Внутренний API скоринга и сохранение его результатов | 8082 |
| `document-service` | Получение команд из Kafka, создание и хранение PDF | 8083 |
| Keycloak | Пользователи, роли и токены | 8180 |
| Kafka | События запроса и завершения генерации документов | 9092 |

Бэкенд — многомодульная Gradle-сборка на Java 21 и Spring Boot 3.2.5. Для хранения данных используются PostgreSQL 16, Spring Data JPA и Liquibase. Общие модели событий и внутреннего API находятся в `backend/libs/contracts`, правила и расчёты — в `backend/libs/domain/calculator-engine`.

Заявка проходит состояния `DRAFT → SCORING_COMPLETED → OFFER_SELECTED → DOCUMENTS_REQUESTED → DOCUMENTS_READY → CONTRACT_SIGNED`. Проверки могут завершиться отказом: `PRESCORING_REJECTED` или `SCORING_REJECTED`. После скоринга изменение исходных данных заявки запрещено. Изменения состояния выполняются с блокировкой записи; повторные события документов обрабатываются без повторного сохранения одного документа.

## Разработка и Devin

Работа велась поэтапно: интерфейс калькулятора и авторизации, выделение серверных сервисов, подключение Keycloak и Kafka, реализация жизненного цикла заявки, затем рефакторинг и исправления.

Devin использовался как помощник для реализации серверного этапа и интеграции интерфейса с сервисами. Результат этого этапа можно посмотреть в [pull request № 2](https://github.com/dpnevsky/credit-calculator/pull/2). В последующих изменениях также использовался Codex. Работа с ассистентами включала постановку задач, проверку результатов, уточнения и доработку кода; сгенерированные изменения проверяются сборкой и тестами.

Ветки:

- [`main`](https://github.com/dpnevsky/credit-calculator/tree/main) — объединённая версия проекта.
- [`develop`](https://github.com/dpnevsky/credit-calculator/tree/develop) — интеграция изменений и дальнейшая разработка.
- `feature/*` — отдельные задачи.
- [`devin/1775077694-backend-stage1-completion`](https://github.com/dpnevsky/credit-calculator/tree/devin/1775077694-backend-stage1-completion) — рабочая ветка этапа с Devin; её разработка перенесена в `develop`.

Для следующих задач: отдельная ветка от `develop`, изменение, проверки и pull request в `develop`; проверенная версия объединяется в `main`.

## Локальный запуск

Нужны JDK 21, Node.js 22.18+ и Docker с Compose. Команды выполняются из корня репозитория, если не указано иначе.

### 1. Инфраструктура

```bash
docker compose -f backend/infra/compose/docker-compose.local.yml up -d
```

Compose запускает Kafka, Keycloak и четыре базы PostgreSQL. Порты баз: 5433 — заявки, 5434 — скоринг, 5435 — документы, 5436 — Keycloak. Realm `credit-calculator` импортируется автоматически при первом запуске.

Создайте два используемых топика:

```bash
docker compose -f backend/infra/compose/docker-compose.local.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic document-generation-requested --partitions 3 --replication-factor 1
docker compose -f backend/infra/compose/docker-compose.local.yml exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --create --if-not-exists --topic document-generated --partitions 3 --replication-factor 1
```

Дождитесь запуска Keycloak: <http://localhost:8180>. Изменения файла realm не заменяют уже импортированный realm в существующей базе; их нужно перенести через настройки Keycloak.

### 2. Сервисы

Запустите каждую команду в отдельном терминале из каталога `backend`:

```bash
./gradlew :apps:scoring-service:bootRun
./gradlew :apps:document-service:bootRun
./gradlew :apps:application-service:bootRun
./gradlew :apps:api-gateway:bootRun
```

В Windows используется `gradlew.bat`. Схема баз создаётся Liquibase при запуске. PDF по умолчанию сохраняются в `build/document-storage` относительно рабочего каталога `document-service`; путь можно задать свойством `app.document-storage.base-path`.

### 3. Интерфейс

```bash
cd frontend
npm ci
npm run dev
```

Откройте <http://localhost:3000>, зарегистрируйтесь и создайте заявку. Vite перенаправляет `/api` на gateway. Калькулятор на главной странице доступен без входа.

## Проверки

```bash
cd backend
./gradlew build
```

```bash
cd frontend
npm ci
npm test
npm run lint
npm run build
```

Java-тесты проверяют расчёты, правила скоринга, валидацию, операции с заявками и события документов. Тесты фронтенда проверяют выход при сетевом сбое и обновление сессии. Эти проверки не заменяют сквозной запуск с PostgreSQL, Kafka и Keycloak.

## Текущее состояние

Это учебный MVP с локальной конфигурацией. Пароли в Compose, тестовые пользователи realm и внутренний ключ документов предназначены для локального запуска. Внутренний API скоринга должен быть доступен только сервисам. Токены и черновики форм сохраняются в браузере; выход очищает токены и черновики создания заявки.

Следующие направления развития: сквозные интеграционные тесты, привязка владельца заявки к стабильному `sub` пользователя, Authorization Code + PKCE или BFF вместо password grant, ограниченные сервисные учётные записи Keycloak и transactional outbox для согласования записи в PostgreSQL с публикацией в Kafka. Сейчас отправка события ожидает подтверждения брокера, но единой транзакции между базой и Kafka нет.
