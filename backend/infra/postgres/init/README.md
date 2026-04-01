# PostgreSQL Initialization

Each microservice manages its own database via Liquibase migrations.
Databases are created automatically by Docker Compose.

## Local databases

| Service              | Database         | Port  | User               |
|----------------------|------------------|-------|--------------------|
| application-service  | application_db   | 5433  | application_user   |
| scoring-service      | scoring_db       | 5434  | scoring_user       |
| document-service     | document_db      | 5435  | document_user      |
| keycloak             | keycloak_db      | 5436  | keycloak_user      |
