---
name: Milestone 1 Backend Skeleton (minimal foundation)
overview: Bootstrap the Spring Boot 3 project with PostgreSQL configuration, Flyway setup, health endpoint, CI/CD pipeline, and Docker containerization. Tests run without database dependencies. This establishes the minimal foundation for subsequent milestones.
todos:
  - id: m1-pom
    content: Create pom.xml with Java 21, Spring Boot 3, PostgreSQL, Flyway dependencies
    status: pending
  - id: m1-config
    content: Create application.yml with PostgreSQL and Flyway configuration
    status: pending
  - id: m1-main
    content: Create RoleMarkApplication.java main class
    status: pending
  - id: m1-health
    content: Implement HealthController with GET /api/health endpoint
    status: pending
  - id: m1-flyway
    content: Create minimal V1__init.sql Flyway migration (empty or comment only)
    status: pending
  - id: m1-docker
    content: Create multi-stage Dockerfile and docker-compose.yml with healthchecks
    status: pending
  - id: m1-ci
    content: Set up GitHub Actions workflow that runs mvn -B -ntp test without database
    status: pending
  - id: m1-test
    content: Write HealthControllerTest using @WebMvcTest with MockMvc (no DB required)
    status: pending
  - id: m1-gitignore
    content: Create .gitignore for Maven, IDE, and OS files
    status: pending
  - id: m1-env-example
    content: Create .env.example documenting required environment variables
    status: pending
---

# Milestone 1: Backend Skeleton (minimal foundation)

## Overview

Set up the foundational Spring Boot 3 project with database configuration, migration management, health monitoring, CI/CD pipeline, and containerization. Tests are isolated and do not require a running database. No business logic beyond the health endpoint.

## Project Structure

```
RoleMark/
├── pom.xml                          # Maven config (Java 21, Spring Boot 3)
├── Dockerfile                        # Multi-stage container image definition
├── docker-compose.yml                # Local dev environment (PostgreSQL + app)
├── .gitignore                        # Maven, IDE, OS exclusions
├── .env.example                      # Environment variable template (committed)
├── .github/workflows/ci.yml          # GitHub Actions build/test pipeline
└── src/
    ├── main/
    │   ├── java/com/rolemark/
    │   │   ├── RoleMarkApplication.java
    │   │   └── controller/
    │   │       └── HealthController.java
    │   └── resources/
    │       ├── application.yml       # DB config, server port, etc.
    │       └── db/migration/
    │           └── V1__init.sql      # Minimal migration (empty or comment)
    └── test/
        └── java/com/rolemark/
            └── controller/
                └── HealthControllerTest.java
```

## Implementation Tasks

### 1. Maven Project Setup

- **File**: `pom.xml`
- Java 21, Spring Boot 3.x (latest stable)
- Dependencies:
  - `spring-boot-starter-web`
  - `spring-boot-starter-jdbc`
  - `spring-boot-starter-validation`
  - `postgresql` driver
  - `flyway-core`
  - `spring-boot-starter-test`
- Maven wrapper (mvnw/mvnw.cmd)
- **Note**: `spring-boot-starter-jdbc` provides DataSource for Flyway without introducing ORM/JPA. JPA, repositories, and ORM-related dependencies are NOT included in Milestone 1. Database integration is limited to configuration and Flyway presence only.

### 2. Application Configuration

- **File**: `src/main/resources/application.yml`
- PostgreSQL connection configuration (host, port, database, username, password via environment variables)
- Flyway enabled with migration location: `db/migration`
- Flyway runs automatically when a DataSource is present. Flyway is NOT disabled in application.yml.
- Server port: 8080 (default)
- Basic logging configuration
- Use environment variables for sensitive values (no hardcoded credentials)

### 3. Main Application Class

- **File**: `src/main/java/com/rolemark/RoleMarkApplication.java`
- Standard Spring Boot main class with `@SpringBootApplication`
- Package: `com.rolemark`

### 4. Health Endpoint

- **File**: `src/main/java/com/rolemark/controller/HealthController.java`
- `GET /api/health` → `{"status":"ok"}`
- Simple REST controller with `@RestController` and `@RequestMapping("/api")`
- No service layer needed for M1

### 5. Flyway Migration

- **File**: `src/main/resources/db/migration/V1__init.sql`
- Minimal content: empty file or single comment line (e.g., `-- Initial schema will be added in Milestone 2`)
- Do NOT create any tables, indexes, or database objects
- Flyway will automatically create its metadata tables on first run

### 6. Docker Configuration

- **File**: `Dockerfile`
  - Multi-stage build:
    - Stage 1: Build (Java 21 JDK, Maven, copy pom.xml and src, build JAR)
    - Stage 2: Runtime (Java 21 JRE, copy JAR, expose port 8080, set entrypoint)
  - Use ARG-based approach for base images: `ARG BUILD_IMAGE=eclipse-temurin:21-jdk` and `ARG RUNTIME_IMAGE=eclipse-temurin:21-jre`
  - Build stage: `FROM ${BUILD_IMAGE} AS build`
  - Runtime stage: `FROM ${RUNTIME_IMAGE}`
  - Non-Alpine is the default; Alpine variants (`-alpine`) are optional optimizations but not recommended as default due to JVM stability, SSL/DNS reliability concerns
  - Copy only necessary files to minimize image size

- **File**: `docker-compose.yml`
  - PostgreSQL service:
    - Image: `postgres:15-alpine`
    - Environment variables: `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
    - Healthcheck: `pg_isready -U ${POSTGRES_USER}` (POSTGRES_USER is defined in `.env.example` and used consistently in both `environment:` and healthcheck)
    - Volume: `postgres_data` for data persistence
  - RoleMark app service:
    - Build from Dockerfile
    - Environment variables for DB connection (read from `.env` or compose file)
    - `depends_on` with `condition: service_healthy` for PostgreSQL
    - Port mapping: `8080:8080`
  - Network: default bridge network
  - All credentials via environment variables (no hardcoded values)
  - **Note**: docker-compose.yml is for LOCAL development only, not production deployment

### 7. GitHub Actions CI

- **File**: `.github/workflows/ci.yml`
- Trigger: on push and pull_request to main branch
- Steps:
  1. Checkout code
  2. Set up Java 21 (using `actions/setup-java@v4` with `distribution: 'temurin'`)
  3. Run `mvn -B -ntp test` (this compiles and runs tests successfully)
- CI must pass without a database connection
- Fail build on any step failure
- Do NOT run separate build and test commands (single `mvn -B -ntp test` command is sufficient)

### 8. Health Endpoint Test

- **File**: `src/test/java/com/rolemark/controller/HealthControllerTest.java`
- **MUST use** `@WebMvcTest(HealthController.class)` (NOT `@SpringBootTest`)
- Use `MockMvc` to test the endpoint
- Verify:
  - Status code: 200
  - Response body: `{"status":"ok"}`
- Test must run without PostgreSQL or Flyway
- No database dependencies in test configuration
- Test must pass in GitHub Actions with no services running

### 9. Git Configuration

- **File**: `.gitignore`
- Include standard Maven ignores: `target/`, `.mvn/wrapper/maven-wrapper.jar`
- IDE files: `.idea/`, `.vscode/`, `*.iml`, `.classpath`, `.project`, `.settings/`
- OS files: `.DS_Store`, `Thumbs.db`
- Environment files: `.env`, `.env.local`, `.env.*.local`
- **Note**: `.env` MUST be ignored. Use `.env.example` (committed) to document required variables without secrets.

- **File**: `.env.example`
- Template file documenting all required environment variables
- Include variable names with example/placeholder values (no real secrets)
- The following environment variables MUST be defined exactly as named:
  - `POSTGRES_DB` - PostgreSQL database name
  - `POSTGRES_USER` - PostgreSQL username (used in both docker-compose PostgreSQL service and healthcheck)
  - `POSTGRES_PASSWORD` - PostgreSQL password
  - `SPRING_DATASOURCE_URL` - Spring DataSource JDBC URL (e.g., `jdbc:postgresql://postgres:5432/rolemark`)
  - `SPRING_DATASOURCE_USERNAME` - Spring DataSource username (typically matches `POSTGRES_USER`)
  - `SPRING_DATASOURCE_PASSWORD` - Spring DataSource password (typically matches `POSTGRES_PASSWORD`)
- `SPRING_DATASOURCE_*` variables are set independently in the RoleMark app service environment, but should match the corresponding `POSTGRES_*` values for consistency
- This file is committed to version control

## Validation Criteria

- `mvn -B -ntp test` passes (compiles and runs tests successfully)
- Health endpoint test uses `@WebMvcTest(HealthController.class)` and does not require database
- Test passes without PostgreSQL running
- Docker Compose starts PostgreSQL and app successfully
- Health endpoint returns `{"status":"ok"}` at `http://localhost:8080/api/health` when running via docker-compose
- GitHub Actions workflow passes on push/PR (runs `mvn -B -ntp test` without database)
- All configuration uses environment variables (no hardcoded credentials)

## Validation Commands

### 1) Run Maven tests (compiles and runs tests successfully)
```powershell
mvn -B -ntp test
```

## Out of Scope (Deferred to Later Milestones)

- Authentication/authorization (M2)
- JPA dependencies (`spring-boot-starter-data-jpa`) and ORM functionality (M2+)
- Database entities, JPA repositories, or tables (M2+)
- Business logic endpoints (M2+)
- Security configuration beyond Spring Boot defaults (M2)
- Service layer architecture (M2+)
- Error handling framework (M2)
- UI/Thymeleaf templates (M5)
- Landing page (M6)
- README documentation beyond basic project description (M7)
- Any webapp directory or static resources (M5+)
- Any scaffolding or placeholder code for future milestones

## Notes

- Package naming: `com.rolemark.*` (consistent with project name RoleMark)
- All sensitive configuration externalized via environment variables
- Docker Compose is for local development only (not production deployment)
- CI runs tests in isolation without external dependencies
- Flyway migrations will be populated in Milestone 2 when entities are defined
- No scaffolding or placeholder code for future milestones
- Tests use `@WebMvcTest` to avoid loading full Spring context and database dependencies
- Milestone 1 does NOT include JPA or ORM dependencies; database integration is configuration-only
- Non-Alpine Java base images are preferred for JVM stability and SSL/DNS reliability

## Git Milestone Commands

Upon completion and validation, stage and commit with these commands:

```bash
git add .
git commit -m "chore: bootstrap Spring Boot project with PostgreSQL, Flyway, and CI"
```
