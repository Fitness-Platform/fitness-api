# Fitness Platform API

Backend REST API for the Fitness Platform.

The application is being developed as a modular monolith using Java and Spring Boot.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Maven
- Docker Compose
- JUnit
- Testcontainers
- GitHub Actions

## Requirements

The following tools are required for local development:

- Java 21
- Docker
- Docker Compose
- Git

A global Maven installation is not required because the project includes the Maven Wrapper.

You can verify Java with:

```bash
java -version
```

The project expects Java 21.

## Clone the Repository

```bash
git clone <repository-url>
cd fitness-api
```

## Environment Configuration

Create the local environment file from the provided example:

```bash
cp .env.example .env
```

Default values:

```env
POSTGRES_DB=fitness
POSTGRES_USER=fitness
POSTGRES_PASSWORD=fitness_dev
POSTGRES_PORT=5432
```

The `.env` file is ignored by Git and should not be committed.

If port `5432` is already in use on your machine, change only your local `.env`, for example:

```env
POSTGRES_PORT=5434
```

The versioned `.env.example` should continue representing the default development configuration.

## Start PostgreSQL

Start the local PostgreSQL container:

```bash
docker compose up -d
```

Check its status:

```bash
docker compose ps
```

The PostgreSQL service should eventually report a healthy status.

To inspect the logs:

```bash
docker compose logs postgres
```

To stop the local environment:

```bash
docker compose down
```

The PostgreSQL data is stored in a named Docker volume and is preserved by a regular `docker compose down`.

Running the following command also removes the database volume:

```bash
docker compose down -v
```

Use it only when you intentionally want to recreate the local database from scratch.

## Run the Application

The Spring Boot application reads the PostgreSQL configuration from environment variables.

When running from a terminal, export the variables from `.env`:

```bash
set -a
source .env
set +a
```

Then start the application:

```bash
./mvnw spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

There are no business endpoints in the Foundation phase yet, so receiving `404` for `/` is expected.

### IntelliJ IDEA

When running the application directly through IntelliJ IDEA, configure the same environment variables in the application's Run Configuration.

Example:

```text
POSTGRES_DB=fitness
POSTGRES_USER=fitness
POSTGRES_PASSWORD=fitness_dev
POSTGRES_PORT=5432
```

Use the port configured in your local `.env`.

## Admin Bootstrap

The V1 platform supports two roles:

- `USER`
- `ADMIN`

Newly registered accounts are always created with the `USER` role. Clients cannot choose or assign an administrative role during registration.

For V1, the initial administrator is promoted operationally after registration.

Example:

```sql
UPDATE users
SET role = 'ADMIN'
WHERE email = '<admin-email>';
```

This operation must only be performed by an authorized operator with direct database access.

There is no public role-management or admin-promotion API in V1.

## Database Migrations

Flyway manages database schema evolution.

Migration files belong in:

```text
src/main/resources/db/migration
```

Migration files follow the Flyway naming convention:

```text
V1__description.sql
V2__description.sql
V3__description.sql
```

Applied migrations must not be modified. Schema changes should be introduced through new migrations.

Hibernate does not manage the database schema automatically.

## Run Tests

The automated test suite uses Testcontainers.

A manually running PostgreSQL development container is not required for tests.

The Docker daemon must be available because Testcontainers creates an isolated PostgreSQL container automatically.

Run:

```bash
./mvnw clean test
```

A successful execution should finish with:

```text
BUILD SUCCESS
```

## Continuous Integration

Pull Requests targeting `main` are automatically validated through GitHub Actions.

The CI workflow:

1. checks out the repository;
2. configures Java 21;
3. restores the Maven dependency cache when available;
4. runs `./mvnw clean test`;
5. starts PostgreSQL automatically through Testcontainers.

The backend CI check must pass before changes can be merged into `main`.

## Project Structure

Current high-level structure:

```text
fitness-api/
├── .github/
│   └── workflows/
├── docs/
│   └── architecture/
├── src/
│   ├── main/
│   │   ├── java/
│   │   └── resources/
│   │       └── db/
│   │           └── migration/
│   └── test/
├── compose.yaml
├── .env.example
├── pom.xml
├── mvnw
└── README.md
```

The backend follows a feature-first modular monolith organization.

See:

```text
docs/architecture/package-structure.md
```

for the package organization guidelines.

## Development Workflow

Development follows a Pull Request based workflow.

```text
Issue
  ↓
Branch
  ↓
Implementation
  ↓
Tests
  ↓
Pull Request
  ↓
Backend CI
  ↓
Review
  ↓
Squash and merge
  ↓
main
```

Create branches from an updated `main`:

```bash
git switch main
git pull origin main
git switch -c <branch-name>
```

Examples:

```text
feature/add-authentication
fix/program-access-expiration
test/add-registration-integration-tests
docs/update-api-documentation
```

Direct development on `main` should be avoided.

## Current Project Status

The project is currently in its Foundation phase.

The backend infrastructure includes:

- Spring Boot application bootstrap
- PostgreSQL local development environment
- Spring Data JPA
- Flyway
- Testcontainers
- GitHub Actions CI
- modular monolith package guidelines

Business features will be introduced incrementally in subsequent phases.