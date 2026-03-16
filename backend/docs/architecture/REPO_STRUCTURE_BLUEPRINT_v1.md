# REPO_STRUCTURE_BLUEPRINT_v1

## 1. Purpose

This document defines the practical monorepo structure for the `credit-calculator` project based on the already fixed architecture baseline and MVP blueprint.

It covers:

- monorepo directory layout
- deployable application modules
- shared library modules
- internal service structure
- infra/config/deployment directories
- rules for shared contracts
- preparation for future Kubernetes deployment

This structure is intended to be:

- production-like
- realistic for one developer
- compatible with future project growth
- aligned with the current MVP architecture

---

## 2. Architectural assumptions

This blueprint is based on the following fixed decisions:

- `api-gateway` is the only public backend entrypoint
- `application-service` is the main business orchestrator and owner of application lifecycle
- `scoring-service` is an internal synchronous scoring engine
- `document-service` is an asynchronous document generation service
- `Keycloak` is the single identity provider
- `Kafka` is used only where asynchronous interaction is justified
- each service owns its own PostgreSQL database
- direct cross-service database access is forbidden
- document generation must be extensible beyond PDF/XML in the future
- shared code must stay thin and must not become a fat core

---

## 3. Monorepo structure

Recommended repository layout:

```text
credit-calculator/
├─ apps/
│  ├─ api-gateway/
│  ├─ application-service/
│  ├─ scoring-service/
│  ├─ document-service/
│  └─ web-client/                          # optional later
│
├─ libs/
│  ├─ contracts/
│  │  ├─ event-envelope/
│  │  ├─ document-events/
│  │  └─ scoring-internal-api/
│  └─ testing/
│     └─ test-support/
│
├─ infra/
│  ├─ compose/
│  │  ├─ docker-compose.local.yml
│  │  └─ docker-compose.infra.yml
│  ├─ keycloak/
│  │  ├─ realm/
│  │  ├─ clients/
│  │  └─ users/
│  ├─ kafka/
│  │  ├─ topics/
│  │  └─ init/
│  ├─ postgres/
│  │  └─ init/
│  └─ storage/
│     ├─ filesystem/
│     └─ minio/                            # optional later
│
├─ deploy/
│  └─ k8s/
│     ├─ base/
│     │  ├─ api-gateway/
│     │  ├─ application-service/
│     │  ├─ scoring-service/
│     │  └─ document-service/
│     └─ overlays/
│        ├─ local/
│        ├─ dev/
│        └─ prod/
│
├─ docs/
│  ├─ architecture/
│  ├─ adr/
│  ├─ api/
│  ├─ events/
│  └─ runbooks/
│
├─ scripts/
├─ .github/
│  └─ workflows/
├─ gradle/
├─ build-logic/                            # optional later
├─ settings.gradle.kts
├─ build.gradle.kts
└─ gradle/libs.versions.toml

4. Directory responsibilities
4.1 apps/

Contains all deployable application modules.

Rules:

each backend service is a separate deployable application

each application has its own build file, Dockerfile, config, migrations, and codebase

no business logic is shared through apps/

only deployable units belong here

Current expected applications:

api-gateway

application-service

scoring-service

document-service

Optional later:

web-client

4.2 libs/

Contains only reusable code that is actually shared by more than one module.

Rules:

keep libraries very thin

no shared domain model

no shared persistence model

no shared business logic

no generic core, common-core, backend-core, or similar fat modules

Current expected library groups:

contracts/

testing/

Optional later libraries may appear only if duplication becomes real and stable.

4.3 infra/

Contains local/integration infrastructure assets and configuration.

Rules:

infrastructure files must not be mixed into service code directories

local Docker Compose, Keycloak config, Kafka init scripts, and storage bootstrap files belong here

infra here is primarily for local development, integration testing, and operational reproducibility

This directory is not the same as Kubernetes deployment manifests.

4.4 deploy/

Contains deployment descriptors for Kubernetes.

Rules:

keep deployment manifests separate from service code

start with application components only

infrastructure components like Kafka/Postgres/Keycloak may be deployed separately later depending on chosen deployment model

use base/ and overlays/ to support environment-specific evolution

4.5 docs/

Contains architecture and engineering documentation.

Recommended content:

architecture diagrams

ADRs

API descriptions

Kafka event schemas

runbooks

local startup instructions

operational notes

5. Deployable modules
5.1 apps/api-gateway
Role

Thin edge service.

Responsibilities

single external backend entrypoint

JWT validation for public requests

route-level authorization

request routing

edge filters

correlation-id propagation

CORS and security headers

public API boundary

Must not do

business logic

orchestration of credit flow

direct DB access

scoring logic

document generation logic

user profile business logic

Suggested internal structure
apps/api-gateway/
├─ src/main/java/.../gateway/
│  ├─ config/
│  ├─ security/
│  ├─ routing/
│  ├─ filters/
│  ├─ error/
│  └─ bootstrap/
├─ src/main/resources/
│  ├─ application.yml
│  ├─ application-local.yml
│  └─ logback-spring.xml
├─ Dockerfile
└─ build.gradle.kts
5.2 apps/application-service
Role

Main business workflow service.

Responsibilities

create application

update draft application

submit application

local prescoring

offer generation and selection

synchronous scoring orchestration

asynchronous document generation orchestration

application lifecycle state management

public business API for clients

consumption of document result events

lifecycle/integration event publication

Owns

application state

applicant data

prescoring results

offers

application-document linkage

application lifecycle statuses

Must not do

implement full scoring engine internals

generate PDF/XML directly

read another service database directly

Suggested internal structure
apps/application-service/
├─ src/main/java/.../application/
│  ├─ api/
│  │  └─ rest/
│  ├─ application/
│  │  ├─ command/
│  │  ├─ query/
│  │  ├─ service/
│  │  └─ port/
│  │     ├─ in/
│  │     └─ out/
│  ├─ domain/
│  │  ├─ application/
│  │  ├─ prescoring/
│  │  ├─ offers/
│  │  ├─ payment/
│  │  └─ common/
│  ├─ infrastructure/
│  │  ├─ persistence/
│  │  ├─ messaging/
│  │  │  ├─ outbox/
│  │  │  └─ inbox/
│  │  ├─ client/
│  │  │  └─ scoring/
│  │  ├─ security/
│  │  ├─ mapper/
│  │  └─ config/
│  └─ bootstrap/
├─ src/main/resources/
│  ├─ db/migration/
│  ├─ application.yml
│  ├─ application-local.yml
│  └─ logback-spring.xml
├─ Dockerfile
└─ build.gradle.kts
Notes

workflow behavior should be implemented through use cases/services, not fat controllers

prescoring remains inside this service

scoring transport must be abstracted behind a ScoringClient port

document generation must be initiated through messaging, not internal HTTP callbacks

5.3 apps/scoring-service
Role

Internal synchronous scoring engine.

Responsibilities

accept internal scoring request

calculate decision / score / risk grade

store scoring requests and results

support scoring strategy/rules versioning

return scoring result synchronously

Owns

scoring request history

scoring result history

scoring strategy/rules versions

Must not do

own application lifecycle

expose public client API

become the main orchestrator

require Kafka for the main MVP flow

Suggested internal structure
apps/scoring-service/
├─ src/main/java/.../scoring/
│  ├─ api/
│  │  └─ internal/
│  │     └─ rest/
│  ├─ application/
│  │  ├─ service/
│  │  └─ port/
│  │     ├─ in/
│  │     └─ out/
│  ├─ domain/
│  │  ├─ scoring/
│  │  ├─ rules/
│  │  ├─ policy/
│  │  └─ result/
│  ├─ infrastructure/
│  │  ├─ persistence/
│  │  ├─ mapper/
│  │  ├─ security/
│  │  └─ config/
│  └─ bootstrap/
├─ src/main/resources/
│  ├─ db/migration/
│  ├─ application.yml
│  └─ logback-spring.xml
├─ Dockerfile
└─ build.gradle.kts
Notes

no public ingress except internal service network

no Kafka producer/consumer required in MVP

keep internal API explicit and version-friendly

5.4 apps/document-service
Role

Asynchronous document generation subsystem.

Responsibilities

consume document generation requests from Kafka

generate PDF/XML

support future extensibility to new formats

store document metadata

store files through storage abstraction

provide document read/download API

publish document result events

Owns

document generation requests

document metadata

generation state

template versioning

storage integration

Must not do

own application lifecycle

depend on direct reads from application-service DB

expose generation callbacks through HTTP in MVP

Suggested internal structure
apps/document-service/
├─ src/main/java/.../document/
│  ├─ api/
│  │  └─ rest/
│  ├─ application/
│  │  ├─ command/
│  │  ├─ query/
│  │  ├─ service/
│  │  └─ port/
│  │     ├─ in/
│  │     └─ out/
│  ├─ domain/
│  │  ├─ document/
│  │  ├─ template/
│  │  ├─ generation/
│  │  └─ format/
│  ├─ infrastructure/
│  │  ├─ messaging/
│  │  │  ├─ consumer/
│  │  │  ├─ producer/
│  │  │  ├─ outbox/
│  │  │  └─ inbox/
│  │  ├─ persistence/
│  │  ├─ storage/
│  │  │  ├─ filesystem/
│  │  │  └─ s3/
│  │  ├─ rendering/
│  │  │  ├─ pdf/
│  │  │  └─ xml/
│  │  ├─ security/
│  │  └─ config/
│  └─ bootstrap/
├─ src/main/resources/
│  ├─ db/migration/
│  ├─ templates/
│  ├─ application.yml
│  └─ logback-spring.xml
├─ Dockerfile
└─ build.gradle.kts
Notes

generation starts only from Kafka command consumption in MVP

download/read API stays here

rendering must be strategy/factory based, not hardcoded if/else branching

6. Shared library modules
6.1 Required from the start
libs/contracts/event-envelope

Contains only generic transport metadata.

Examples:

EventEnvelope

EventMetadata

shared event serialization primitives

Must not contain:

business domain logic

service-specific repositories

shared business enums unrelated to transport

libs/contracts/document-events

Contains only document-related Kafka event contracts.

Examples:

DocumentGenerationRequested

DocumentGenerated

DocumentGenerationFailed

Must not contain:

document generation logic

file storage logic

template logic

libs/contracts/scoring-internal-api

Contains only the synchronous internal scoring transport contract.

Examples:

ScoringEvaluationRequest

ScoringEvaluationResponse

Must not contain:

scoring rules

scoring policies

business orchestration logic

libs/testing/test-support

Contains testing helpers used by multiple services.

Examples:

common testcontainers bootstrap

JSON fixtures helpers

event assertion helpers

base integration test utilities

Must not become a giant test framework.

6.2 Optional later only if duplication becomes real

Do not create these immediately unless repeated, stable patterns appear:

shared transactional messaging support

shared security support

shared observability support

common HTTP client wrappers

If they appear, keep them technical and narrow.

7. How to organize shared contracts without a fat core

Recommended structure:

libs/contracts/
├─ event-envelope/
├─ document-events/
└─ scoring-internal-api/

Rules:

Shared contracts are transport contracts only.

Public REST DTOs should usually stay inside the owning service.

Do not move domain entities into shared libs.

Do not move JPA models into shared libs.

Do not create a universal common-dto or core-api module.

If a class is used by only one service, it is not shared.

Version contracts explicitly when needed.

This keeps ownership clear and avoids accidental coupling.

8. Internal service modularity rules

Build-system rule:

one deployable service = one Gradle application module

Code-structure rule:

use strict package-level modularity inside each service

Recommended layered structure inside a service:

api -> application -> domain -> infrastructure

Additional rule:

organize by business capability inside the domain/application layers where appropriate

do not build one giant global controller/service/repository package tree across the whole service

do not split every package into a separate Gradle submodule

Why:

easier local development

simpler build setup

less Gradle overhead

still preserves architectural boundaries

realistic for one developer

9. Infrastructure structure

Recommended infra/ layout:

infra/
├─ compose/
│  ├─ docker-compose.local.yml
│  └─ docker-compose.infra.yml
├─ keycloak/
│  ├─ realm/
│  ├─ clients/
│  └─ users/
├─ kafka/
│  ├─ topics/
│  └─ init/
├─ postgres/
│  └─ init/
└─ storage/
   ├─ filesystem/
   └─ minio/
Responsibilities

compose/ — local development orchestration

keycloak/ — realm export/import and bootstrap config

kafka/ — topic bootstrap/init helpers

postgres/ — init helpers if needed

storage/ — local storage setup and optional future MinIO bootstrap

Rules:

service DB migrations stay inside each service module

only infra bootstrapping belongs here

do not put Flyway/Liquibase migrations here

do not duplicate service-specific config here unless it is environment wiring

10. Kubernetes preparation

Recommended structure:

deploy/k8s/
├─ base/
│  ├─ api-gateway/
│  ├─ application-service/
│  ├─ scoring-service/
│  └─ document-service/
└─ overlays/
   ├─ local/
   ├─ dev/
   └─ prod/
Rules

start with application components

keep manifests environment-neutral in base/

environment-specific adjustments go to overlays

use Kubernetes readiness/liveness compatible service configs from the beginning

do not assume Kafka/Postgres/Keycloak must live in Kubernetes from day one

Preparation principles

Each service should be ready for Kubernetes by design:

stateless runtime behavior

externalized config

health endpoints

structured logs

graceful shutdown

container-friendly startup

11. Documentation structure

Recommended documentation directories:

docs/
├─ architecture/
├─ adr/
├─ api/
├─ events/
└─ runbooks/
Suggested content

architecture/ — diagrams, service maps, interaction overviews

adr/ — architecture decision records

api/ — public/internal API notes and OpenAPI references

events/ — Kafka topics, event schemas, envelope conventions

runbooks/ — local startup, troubleshooting, operational notes

12. Root Gradle modules

Recommended first module set:

apps:api-gateway
apps:application-service
apps:scoring-service
apps:document-service
libs:contracts:event-envelope
libs:contracts:document-events
libs:contracts:scoring-internal-api
libs:testing:test-support

Optional later:

apps:web-client

build-logic

narrow technical libs only if duplication becomes real

13. Implementation priorities
Do first

Create root monorepo skeleton.

Add four application modules.

Add three contracts modules.

Add test-support module.

Create common package skeleton for each service.

Add infra/compose and Keycloak bootstrap.

Add deploy/k8s/base skeleton for application components.

Do not do immediately

create shared-core

create platform libraries “just in case”

over-split services into many Gradle modules

put deployment manifests inside service code

put all DTOs into one common module

build full infra deployment for all stateful components in Kubernetes before service skeleton exists

14. Final rules

Deployable services live in apps/.

Thin shared code lives in libs/.

Local/integration infra lives in infra/.

Kubernetes manifests live in deploy/.

Shared contracts must stay transport-only.

No fat core.

No cross-service database access.

No premature over-modularization.

Service code should be modular by package and bounded responsibility.

Repository structure must support future Kubernetes deployment without forcing premature infrastructure complexity.

15. Final baseline statement

The repository must be structured as a monorepo with:

deployable applications in apps/

thin shared transport/testing libraries in libs/

infrastructure bootstrap/config in infra/

Kubernetes deployment descriptors in deploy/

architecture documentation in docs/