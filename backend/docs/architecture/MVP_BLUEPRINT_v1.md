# MVP_BLUEPRINT_v1

## 1. Scope

### Core services
- `api-gateway`
- `application-service`
- `scoring-service`
- `document-service`

### Infrastructure
- `Keycloak`
- `Kafka`
- `PostgreSQL per service`
- `document storage abstraction`

---

## 2. Responsibility model

### api-gateway
- single external entrypoint
- JWT validation for external requests
- route-level authorization
- request routing
- correlation-id propagation
- no business logic

### application-service
- owner of application lifecycle
- create/update/submit application
- local prescoring
- orchestration of synchronous scoring
- offer management
- orchestration of async document generation
- main API for client applications

### scoring-service
- isolated scoring engine
- synchronous scoring via internal REST
- stores scoring requests/results
- not exposed publicly

### document-service
- async document generation
- PDF/XML generation
- document metadata persistence
- document storage integration
- document download/read model

### Keycloak
- authentication
- token issuance
- roles/scopes
- OIDC/OAuth2 provider

### Kafka
- document generation flow
- minimal lifecycle/integration events
- outbox / inbox / idempotent consumer patterns

---

## 3. Security model

### Authentication and authorization
- `Keycloak` is the single identity provider
- clients authenticate via OIDC/OAuth2 and obtain JWT access tokens
- external access goes only through `api-gateway`

### What `api-gateway` validates
- JWT signature
- `iss`
- `aud`
- expiration / not-before
- required scopes / roles on route level

### Resource servers
- `application-service` must also be a resource server and revalidate JWT for routed user requests
- `document-service` must also be a resource server for routed user document read/download requests
- `scoring-service` does **not** need to be a public resource server in MVP; it is internal-only

### Internal-only components
- `scoring-service` HTTP API
- Kafka brokers/topics
- service databases
- outbox/inbox processing
- document generation consumers

### Internal communication
- `application-service -> scoring-service` goes through internal network only
- `document-service` consumes commands from Kafka only
- no service is exposed directly to the Internet except `api-gateway` and `Keycloak`

---

## 4. Interaction model

```text
[ Web / Mobile Client ]
          |
          v
    [ API Gateway ]
          |
          v
 [ Application Service ] -----------------> [ application-db ]
          |
          | internal REST (sync)
          v
   [ Scoring Service ] --------------------> [ scoring-db ]
          |
          | no public ingress
          |

 [ Application Service ] -- outbox --> [ Kafka ] --> [ Document Service ] --> [ document-db ]
                                                       |
                                                       v
                                                [ Document Storage ]

[ Keycloak ] issues tokens for clients and supports JWT validation in gateway/services.
```

### Main flow
1. Client calls public API through `api-gateway`
2. `application-service` stores and manages application state
3. On business submission, `application-service` runs local prescoring
4. `application-service` calls `scoring-service` synchronously via internal REST
5. `application-service` updates final decision / offers
6. When documents are needed, `application-service` publishes `DocumentGenerationRequested`
7. `document-service` generates documents asynchronously and publishes result events
8. `application-service` updates document linkage/status from Kafka events

---

## 5. Public API

Public API should be business-oriented.

```text
POST   /api/applications
GET    /api/applications/{applicationId}
PATCH  /api/applications/{applicationId}
POST   /api/applications/{applicationId}/submit
GET    /api/applications/{applicationId}/offers
POST /api/applications/{applicationId}/offers/{offerId}/select
POST   /api/applications/{applicationId}/request-documents
GET    /api/applications/{applicationId}/documents
GET    /api/documents/{documentId}/download
```

### API semantics
- `POST /api/applications` — create draft application
- `PATCH /api/applications/{applicationId}` — update application draft data
- `POST /api/applications/{applicationId}/submit` — business submit command; triggers validation, local prescoring, synchronous scoring, and application decision update
- `GET /api/applications/{applicationId}/offers` — list available offers
- `POST /api/applications/{applicationId}/select-offer` — confirm selected offer
- `POST /api/applications/{applicationId}/request-documents` — request document generation for the approved/selected application state
- `GET /api/applications/{applicationId}/documents` — list generated documents
- `GET /api/documents/{documentId}/download` — download generated document

`/prescore` and `/score` are intentionally removed from public API.

---

## 6. Internal HTTP API

Only keep internal endpoints that are still needed in MVP.

### scoring-service
```text
POST /internal/scoring/evaluate
GET  /internal/health
GET  /internal/ready
```

### application-service
```text
GET  /internal/health
GET  /internal/ready
```

### document-service
```text
GET  /internal/health
GET  /internal/ready
```

### Removed from MVP
- internal document generation HTTP endpoints
- internal document attach/callback HTTP endpoints
- internal application context fetch endpoints for document generation

Document flow must go through Kafka events, not internal HTTP callbacks.

---

## 7. Kafka model

### Topics
- `application.events`
- `document.commands`
- `document.events`
- `dead-letter.document.commands`
- `dead-letter.document.events`

### Minimal events in MVP

#### `application.events`
- `ApplicationSubmitted`
- `OfferSelected`
- `ApplicationStatusChanged`

#### `document.commands`
- `DocumentGenerationRequested`

#### `document.events`
- `DocumentGenerated`
- `DocumentGenerationFailed`

### Not used in MVP
- no `scoring.commands`
- no `scoring.events`
- no Kafka request-response for scoring

---

## 8. Producers and consumers

### application-service
Publishes:
- `ApplicationSubmitted`
- `OfferSelected`
- `ApplicationStatusChanged`
- `DocumentGenerationRequested`

Consumes:
- `DocumentGenerated`
- `DocumentGenerationFailed`

### document-service
Publishes:
- `DocumentGenerated`
- `DocumentGenerationFailed`

Consumes:
- `DocumentGenerationRequested`

### scoring-service
- no Kafka producer/consumer required in MVP
- only synchronous internal REST interaction

---

## 9. Reliability patterns

### Required
- `application-service`: outbox + inbox
- `document-service`: outbox + inbox
- idempotent consumers for all document-related Kafka consumers

### Optional in MVP
- `scoring-service`: no outbox/inbox required until scoring becomes async

### Event envelope
Each Kafka event should contain:
- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `producer`
- `correlationId`
- `causationId`
- `payload`

---

## 10. Data ownership

- `application-service` owns application state and offers
- `scoring-service` owns scoring requests/results
- `document-service` owns document metadata and generation state
- each service owns its own PostgreSQL database
- no direct cross-service database access

---

## 11. Evolution path

### Stage 1
- sync scoring via internal REST
- async document generation via Kafka

### Stage 2
- optional async enrichment around scoring
- analytics / audit consumers on lifecycle events

### Stage 3
- full async scoring via Kafka if load or integration complexity justifies it

---

## 12. Final baseline statement

```text
MVP baseline consists of:
- api-gateway as the only public backend entrypoint
- application-service as the main business orchestrator and owner of application lifecycle
- scoring-service as an internal synchronous scoring engine
- document-service as an asynchronous document generation service
- Keycloak as the single identity provider
- Kafka only for document flow and minimal lifecycle/integration events
- PostgreSQL per service with strict data ownership
```
