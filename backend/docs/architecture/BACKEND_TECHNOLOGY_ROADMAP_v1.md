# BACKEND_TECHNOLOGY_ROADMAP_v1

## 1. Purpose

This document defines the backend technology roadmap for the `credit-calculator` project.

It refines the already fixed architecture baseline and MVP blueprint into a practical implementation roadmap that:

- preserves the current service boundaries
- keeps Keycloak as an explicit part of the baseline security model
- treats Kubernetes readiness as a current architectural constraint
- keeps the project realistic for one developer
- maximizes future extensibility without painful rewrites
- supports learning of production-like backend technologies in a coherent way

This roadmap is divided into:

- **Stage 1** — what must be implemented immediately
- **Stage 2** — what must be prepared as explicit extension points
- **Stage 3** — what can be added later without breaking the architecture

---

## 2. Fixed architectural baseline

This roadmap assumes the following decisions are already fixed and must not be revisited without a separate ADR:

- `api-gateway` is the only public backend entrypoint
- `application-service` owns application lifecycle and orchestration
- `scoring-service` performs synchronous scoring via internal REST
- `document-service` generates documents asynchronously via Kafka
- `Keycloak` is the single identity provider
- `Kafka` is used only where asynchronous communication is justified
- each service owns its own PostgreSQL database
- direct cross-service database access is forbidden
- document generation must support `PDF` and `XML` first, while remaining extensible to future formats
- shared code must stay thin and must not grow into a fat shared core

---

## 3. Technology strategy

The project should not optimize for the smallest possible MVP stack.

It should optimize for:

- production-like architecture
- coherent technology choices
- learning value
- future growth without redesign
- realistic implementation scope for one developer

That means:

- use a **uniform baseline** where it improves maintainability and operational consistency
- avoid chaotic technology mixing for the sake of experimenting with many tools at once
- treat advanced technologies as **planned extensions**, not as arbitrary additions

---

## 4. Uniform backend baseline

The following choices should be standardized across the backend platform unless there is a strong documented reason to deviate.

### 4.1 Language and build

- **Java 21**
- **Gradle Kotlin DSL**
- version catalog in `gradle/libs.versions.toml`

### 4.2 Framework family

- **Spring Boot 3.x** as the standard backend framework
- **Spring Cloud Gateway** for `api-gateway`
- **Spring MVC** for `application-service`, `scoring-service`, and `document-service`

### 4.3 Security

- **Keycloak** as the only identity provider
- **Spring Security + OAuth2 Resource Server** where JWT revalidation is required

### 4.4 Persistence

- **PostgreSQL per service**
- **Spring Data JPA / Hibernate** for primary persistence
- **Liquibase** as the single migration tool across backend services

### 4.5 Messaging

- **Kafka** for asynchronous document flow and minimal lifecycle/integration events
- **Spring Kafka** for application integration
- **JSON event envelope** with explicit versioning
- **outbox / inbox** patterns where required by the baseline

### 4.6 Runtime and operations

- **Actuator** for health endpoints
- **structured logs to stdout**
- **Docker** images for all deployable services
- **Kubernetes-ready runtime behavior from the beginning**

---

## 5. Stage 1 — implement immediately

Stage 1 is the first production-like working baseline.

It must already reflect the final architectural direction, even if some advanced platform capabilities are postponed.

## 5.1 Core stack

### Services

- `api-gateway`
- `application-service`
- `scoring-service`
- `document-service`

### Infrastructure

- `Keycloak`
- `Kafka`
- `PostgreSQL per service`
- document storage abstraction

### Primary technology choices

- Java 21
- Spring Boot 3.x
- Spring Cloud Gateway
- Spring MVC
- Spring Security
- Spring Kafka
- Spring Data JPA
- Liquibase
- PostgreSQL
- Docker / Docker Compose
- Kubernetes deployment skeleton in repository

---

## 5.2 Keycloak security model in Stage 1

Keycloak is part of the baseline and must be integrated immediately.

### Required behavior

#### `api-gateway`

Must:

- validate JWT signature
- validate `iss`
- validate `aud`
- validate expiration and `nbf`
- enforce route-level scopes / roles
- propagate correlation id and user context headers where appropriate

Must not:

- contain business logic
- replace service-level authorization where the service owns business semantics

#### `application-service`

Must:

- act as a resource server
- revalidate JWT for routed user requests
- enforce business-level authorization where needed

#### `document-service`

Must:

- act as a resource server
- revalidate JWT for routed document read/download requests

#### `scoring-service`

Stage 1 recommendation:

- keep it **internal-only**
- do not expose it publicly
- do not require it to act as a public resource server in Stage 1

### Why this is the right Stage 1 model

It matches the current baseline, keeps Keycloak explicit, and preserves a clean distinction between:

- external user-facing security
- internal-only service communication

---

## 5.3 Kubernetes readiness requirements in Stage 1

Kubernetes must be treated as a current architectural constraint, not as a late deployment detail.

That means all services should be designed for Kubernetes-compatible runtime behavior from the beginning.

### Required readiness rules

Each service must be:

- stateless in runtime behavior
- externally configurable through environment/config
- compatible with readiness/liveness probes
- able to shut down gracefully
- container-friendly at startup
- able to log to stdout/stderr in a structured format

### Repository requirements

The repository must already contain:

- `deploy/k8s/base/` for application components
- `deploy/k8s/overlays/` for environment evolution
- environment-neutral manifests in `base/`
- no mixing of Kubernetes manifests into service code directories

### Important clarification

Kubernetes readiness does **not** mean that Kafka, Keycloak, and PostgreSQL must all be deployed in Kubernetes in Stage 1.

It means the backend services must be designed so that such a deployment model is possible later without architecture changes.

---

## 5.4 Messaging and reliability in Stage 1

### Required use of Kafka

Kafka is used in Stage 1 for:

- document generation commands
- document generation result events
- minimal lifecycle/integration events where already justified

Kafka is **not** used for scoring request/response in Stage 1.

### Required patterns

#### `application-service`

Must implement:

- outbox
- inbox
- idempotent processing for consumed document events

#### `document-service`

Must implement:

- outbox
- inbox
- idempotent processing for document command consumption

#### `scoring-service`

Does not require Kafka producer/consumer or outbox/inbox in Stage 1.

### Outbox relay recommendation

Stage 1 should use:

- **application-level polling outbox relay**

This relay should:

- read unsent outbox rows
- publish to Kafka
- update send status
- support retry and backoff
- remain separate from business use case code

### Why polling outbox now

It gives the project a real production pattern immediately, while keeping operational complexity realistic for one developer.

---

## 5.5 Persistence baseline in Stage 1

### Recommendation

Use:

- PostgreSQL per service
- Liquibase in each service module
- JPA/Hibernate for ordinary persistence
- JSONB only where snapshots are justified

### JSONB is acceptable for

- outbox payloads
- inbox metadata
- scoring input snapshots
- render context snapshots
- payment schedule snapshots if normalization is not yet necessary

### Liquibase rule

Liquibase changelogs belong **inside each service**, not in `infra/`.

Use one consistent style across services.

Recommended style:

- SQL-first changelogs or disciplined XML/YAML changelogs
- avoid chaotic mixing
- avoid relying on generated diffs as the primary migration workflow

---

## 5.6 Document storage in Stage 1

### Immediate recommendation

Stage 1 should use:

- a `DocumentStoragePort`
- an active **filesystem adapter**

### Mandatory architectural rule

Filesystem must be only a runtime adapter choice.

It must **not** leak into:

- domain model
- API contracts
- business use cases

### Required design shape

`document-service` should use:

- `DocumentStoragePort`
- `FilesystemDocumentStorageAdapter`
- reserved extension point for `S3DocumentStorageAdapter`

### Important runtime rule

Do not treat container filesystem as durable business storage.

In Stage 1 filesystem storage should be treated as:

- external mounted storage
- local development storage
- integration environment storage

---

## 5.7 Document generation technology in Stage 1

Document generation must be implemented as an extensible subsystem, not as two separate hardcoded branches for PDF and XML.

### Recommended structure

Use:

- strategy/factory-based renderer resolution
- template-based rendering
- a shared document generation flow with per-format renderer implementations

### Recommended libraries

#### PDF

- `Thymeleaf` for template rendering to HTML
- `OpenHTMLtoPDF` for HTML-to-PDF generation

#### XML

- `Jackson XML` or JAXB-based serialization

### Required architectural rule

The system should be able to add a new document format later without rewriting the full generation flow.

---

## 5.8 Testing baseline in Stage 1

### Required stack

- JUnit 5
- AssertJ
- Mockito
- Spring Boot Test
- Testcontainers
- Awaitility for async verification
- MockMvc for MVC services
- WebTestClient for gateway tests
- WireMock for internal HTTP interaction tests where useful
- ArchUnit for package boundary checks

### Required test levels

- unit tests for business logic
- integration tests for DB and Kafka behavior
- end-to-end local flow tests for main business slice
- contract-level tests for internal scoring API and document events

---

## 6. Stage 2 — prepare and activate as extension points

Stage 2 is for capabilities that should be planned now but do not need to be fully active in the first working version.

## 6.1 Security evolution

### Recommendation

Add stronger service-to-service security in Stage 2.

### Preferred path

- Keycloak client credentials for machine-to-machine access
- service identities for internal calls
- optional JWT validation in `scoring-service`

### Why Stage 2 and not Stage 1

It is a strong production practice, but it adds extra operational and integration complexity.

It should be added after the first stable end-to-end business flow exists.

---

## 6.2 Storage evolution

### Recommendation

Introduce **MinIO** in Stage 2 as the local/dev S3-compatible runtime.

### Why

This allows the project to learn and validate:

- S3-style storage semantics
- bucket/object access patterns
- object storage configuration and credentials
- smoother eventual move to real S3-compatible storage

### Architectural rule

The move from filesystem to MinIO must require only adapter/config changes, not business redesign.

---

## 6.3 Platform hardening

Stage 2 should activate additional production-like platform capabilities such as:

- Micrometer metrics
- more standardized correlation/logging conventions
- richer CI/CD flow
- OpenAPI publishing
- stricter ArchUnit rules
- resource requests/limits in Kubernetes manifests
- improved retry and dead-letter handling visibility

---

## 6.4 Event ecosystem growth

Stage 2 may also activate:

- analytics consumers
- audit consumers
- broader use of lifecycle events already defined in the baseline

This is useful only after the core business path is stable.

---

## 7. Stage 3 — add later without architecture rewrite

Stage 3 contains technologies that are valuable, but should be adopted only when the project has a stable baseline and a real reason for increased platform complexity.

## 7.1 Debezium / CDC relay

### Recommendation

Treat **Debezium CDC outbox relay** as a later-stage evolution, not as a Stage 1 requirement.

### When it becomes justified

Debezium becomes stronger when:

- event volume increases
- outbox relay operational maturity matters more than simplicity
- the project intentionally expands its Kafka platform learning scope
- Kafka Connect / CDC infrastructure becomes acceptable overhead

### Architectural rule

Stage 1 outbox schema and relay boundary should be designed so that switching to Debezium later does not change business logic.

---

## 7.2 Real S3-compatible storage

### Recommendation

Adopt real S3-compatible storage in Stage 3 for deployed environments.

Examples:

- AWS S3
- cloud-managed S3-compatible storage
- external object storage service

### Why Stage 3

This is a natural continuation after:

- filesystem in Stage 1
- MinIO-backed local/dev learning in Stage 2

---

## 7.3 Async scoring later if justified

### Recommendation

Keep synchronous scoring via internal REST as the Stage 1 baseline.

Later, if load or integration complexity justifies it, Stage 3 may introduce:

- async scoring commands/events
- outbox/inbox for `scoring-service`
- a broader event-driven scoring flow

### Important rule

Do not prematurely build Stage 3 scoring mechanics into Stage 1.

The current architecture already defines a clean evolution path.

---

## 7.4 Observability maturity

Stage 3 is also the right place for:

- OpenTelemetry tracing
- Prometheus / Grafana stack
- advanced dashboards and alerting
- more mature operational telemetry

Actuator and structured logs should still exist from Stage 1.

---

## 8. Trade-off decisions and final recommendations

## 8.1 Flyway vs Liquibase

### Recommendation

Use **Liquibase** as the single migration standard.

### Why

For this project the value is not only delivery speed, but also:

- production-like discipline
- explicit migration management
- future extensibility
- learning value

Liquibase is a valid stronger choice if used consistently.

### Trade-off

#### Liquibase

Pros:

- richer migration semantics
- labels/contexts where useful
- strong learning value for production-like backend work

Cons:

- more boilerplate
- easier to overcomplicate if not disciplined

#### Flyway

Pros:

- simpler baseline
- faster for minimal systems

Cons:

- less expressive for migration workflows
- lower learning value for the chosen project goals

### Final call

Keep **Liquibase** and standardize it across all services.

---

## 8.2 Polling outbox vs Debezium

### Recommendation

- **Stage 1:** polling outbox relay
- **Stage 3:** Debezium only if justified

### Trade-off

#### Polling outbox

Pros:

- easier to implement
- easier to debug
- realistic for one developer
- enough to learn real outbox mechanics

Cons:

- relay polling lag
- custom retry/worker logic

#### Debezium

Pros:

- more mature CDC model
- stronger long-term Kafka platform learning
- avoids custom polling loop logic

Cons:

- more infrastructure complexity
- higher operational overhead
- not necessary for the first working platform slice

### Final call

Do not add Debezium in Stage 1 just for appearance.

Use a properly designed polling outbox now, and keep CDC as a later evolution.

---

## 8.3 Filesystem vs MinIO vs S3

### Recommendation

- **Stage 1:** filesystem
- **Stage 2:** MinIO
- **Stage 3:** real S3-compatible storage

### Trade-off

#### Filesystem

Pros:

- simplest runtime for first implementation
- low operational overhead
- fast feedback loop

Cons:

- does not fully reflect object storage behavior

#### MinIO

Pros:

- strong local/dev approximation of object storage
- good learning value
- smooth bridge to real S3-style storage

Cons:

- more setup than filesystem

#### S3

Pros:

- strongest production target
- real object storage semantics

Cons:

- too early for the first working local platform stage

### Final call

Use filesystem first, but only behind a storage abstraction.

Do not let it become the permanent architectural model.

---

## 8.4 Internal network only vs stronger service-to-service auth

### Recommendation

- **Stage 1:** internal network only for `application-service -> scoring-service`
- **Stage 2:** stronger service-to-service auth through Keycloak client credentials

### Trade-off

#### Internal network only

Pros:

- matches the current baseline
- simpler first implementation
- fewer moving parts for the first stable flow

Cons:

- weaker zero-trust posture
- weaker service identity guarantees

#### Stronger service-to-service auth

Pros:

- stronger production security posture
- clearer machine identity model
- better long-term internal security

Cons:

- more integration complexity
- more token management concerns
- extra startup/debugging overhead

### Final call

Do not force stronger service auth into the first slice if it delays business flow delivery.

But design `scoring-service` so that stronger internal auth can be added later without API redesign.

---

## 9. What must remain uniform in the baseline

The following must remain standardized unless a strong ADR says otherwise:

- Java 21
- Spring Boot family
- Keycloak
- PostgreSQL per service
- Liquibase
- Kafka for async document flow
- JSON event envelope versioning
- thin shared contracts only
- service package layering: `api -> application -> domain -> infrastructure`
- Kubernetes-ready runtime rules

This uniformity is important because the project is not only an application, but also a learning platform for real backend engineering practices.

---

## 10. What can be researched later as separate technologies

The following are valid research/extension topics later, but should not fragment the Stage 1 baseline:

- Debezium CDC
- MinIO and advanced object storage operations
- stronger service-to-service auth
- OpenTelemetry
- Prometheus/Grafana stack
- async scoring
- schema registry / Avro
- platform libraries if duplication becomes real

These should be introduced one by one with explicit purpose, not mixed into the baseline prematurely.

---

## 11. Step-by-step implementation roadmap

## Step 1

Create and stabilize the monorepo skeleton:

- `apps/`
- `libs/`
- `infra/`
- `deploy/`
- `docs/`

Required first modules:

- `apps:api-gateway`
- `apps:application-service`
- `apps:scoring-service`
- `apps:document-service`
- `libs:contracts:event-envelope`
- `libs:contracts:document-events`
- `libs:contracts:scoring-internal-api`
- `libs:testing:test-support`

## Step 2

Add Stage 1 infrastructure assets:

- Docker Compose files
- Keycloak realm/client bootstrap
- Kafka topic bootstrap
- PostgreSQL bootstrap helpers if needed
- filesystem storage bootstrap

## Step 3

Add Kubernetes deployment skeleton immediately:

- `deploy/k8s/base/api-gateway`
- `deploy/k8s/base/application-service`
- `deploy/k8s/base/scoring-service`
- `deploy/k8s/base/document-service`
- `deploy/k8s/overlays/local`
- `deploy/k8s/overlays/dev`
- `deploy/k8s/overlays/prod`

## Step 4

Implement Keycloak-based Stage 1 security baseline:

- gateway JWT validation
- application/document resource server configuration
- route-level authorization
- internal-only scoring-service exposure

## Step 5

Implement the first core business slice:

- application draft/create/update/submit
- local prescoring in `application-service`
- synchronous internal scoring call
- offer generation and persistence

## Step 6

Implement async document flow:

- outbox in `application-service`
- `DocumentGenerationRequested`
- inbox/outbox in `document-service`
- PDF/XML generation through renderer abstraction
- filesystem storage adapter
- `DocumentGenerated` / `DocumentGenerationFailed`
- document status update in `application-service`

## Step 7

Add Stage 1 runtime hardening:

- readiness/liveness
- graceful shutdown
- structured logs
- containerization
- integration tests with Testcontainers

## Step 8

After Stage 1 stabilizes, move to Stage 2 activations:

- client credentials between services
- MinIO
- platform hardening extensions
- richer metrics and event consumers

---

## 12. Final roadmap summary

## Stage 1

Implement now:

- Spring Boot service baseline
- Keycloak security baseline
- Kubernetes-ready runtime behavior
- Liquibase everywhere
- Kafka for document flow only
- polling outbox relay
- filesystem storage through abstraction
- PDF/XML extensible generation flow

## Stage 2

Activate as planned extension points:

- stronger service-to-service auth
- MinIO as S3-compatible local/dev storage
- metrics and platform hardening
- richer lifecycle event usage

## Stage 3

Add later without architecture rewrite:

- Debezium CDC relay
- real S3-compatible storage
- async scoring if justified
- advanced observability stack

---

## 13. Final recommendation

The strongest implementation path for this project is:

- **Stage 1:** build a coherent production-like baseline
- **Stage 2:** activate explicit extension points
- **Stage 3:** expand platform capabilities without rewriting core architecture

This keeps the project realistic, extensible, technically coherent, and valuable as a production-like learning platform.
