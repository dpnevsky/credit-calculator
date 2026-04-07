# Run Guide

## Prerequisites

- Docker Desktop
- Node.js 20+
- npm 10+
- JDK 21 (recommended for backend)

## 1) Start infrastructure

From `backend/infra/compose`:

```powershell
docker compose -f docker-compose.local.yml up -d
```

This starts:
- PostgreSQL databases,
- Kafka,
- Keycloak.

## 2) Start backend services

From `backend`:

```powershell
.\gradlew.bat :apps:application-service:bootRun
.\gradlew.bat :apps:scoring-service:bootRun
.\gradlew.bat :apps:document-service:bootRun
.\gradlew.bat :apps:api-gateway:bootRun
```

If you run backend from IntelliJ, run these applications there instead.

## 3) Start frontend

From `frontend`:

```powershell
npm install
npm run dev
```

Frontend URL: `http://localhost:3000`

## 4) Quick health checks

```powershell
Invoke-WebRequest http://localhost:8080/actuator/health
Invoke-WebRequest http://localhost:3000
Invoke-WebRequest http://localhost:8180/realms/credit-calculator/.well-known/openid-configuration
```

Expected: HTTP `200`.

## 5) Login and registration behavior

- New user: open `/register` and create account.
- Existing user: open `/login` and sign in.
- After sign-in, protected routes become available:
  - `/applications`
  - `/applications/new`
  - `/applications/:applicationId`
  - `/profile`

## 6) Stop everything

- Stop frontend/backend processes in terminal/IDE.
- Stop infrastructure:

```powershell
docker compose -f backend/infra/compose/docker-compose.local.yml down
```
