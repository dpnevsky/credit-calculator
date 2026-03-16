# Credit Calculator — Architecture Baseline v1

## Core architecture
- api-gateway
- application-service
- scoring-service
- document-service

## Infrastructure
- Keycloak
- Kafka
- PostgreSQL per service
- document storage abstraction

## Main decisions
- gateway is included in MVP baseline
- auth-service is not included as a separate deployable component
- prescoring lives in application-service
- scoring lives in scoring-service
- document generation lives in document-service
- documents must support PDF and XML first, with future extensibility to new formats
- architecture should be production-like, microservice-oriented, but without unnecessary fragmentation

## Constraints
- project should be realistic for one developer
- architecture should support future Kubernetes deployment
- architecture should support future mobile client
- Kafka should be used where async communication is justified