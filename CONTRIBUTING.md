# Contributing to Coffer

Thank you for your interest in contributing to Coffer! This guide will help you get the project running locally.

## Project Structure

Coffer is split across multiple repositories:

| Repository | Description |
|---|---|
| [coffer2](https://github.com/verbindolai/coffer2) | Spring Boot backend API (Kotlin) |
| [coffer2-ui](https://github.com/verbindolai/coffer-ui) | Angular frontend |
| [coffer-deploy](https://github.com/verbindolai/coffer-deploy) | Docker Compose production deployment |

## Prerequisites

- **Java 21+** (e.g. [Eclipse Temurin](https://adoptium.net/))
- **Node.js 18+** and **npm**
- **Docker** and **Docker Compose** (for the development database)
- A [Numista API key](https://en.numista.com/api/) (optional, but needed for catalog features)

## Getting Started

### 1. Clone the repositories

```bash
git clone https://github.com/verbindolai/coffer2.git
git clone https://github.com/verbindolai/coffer-ui.git
```

### 2. Start the database

The backend repository includes a Docker Compose file for local development:

```bash
cd coffer2
docker compose -f docker-compose-dev.yml up -d
```

This starts a PostgreSQL 16 instance on `localhost:5432` with:
- **Database:** `coffer`
- **Username:** `coffer`
- **Password:** `coffer`

### 3. Configure environment variables

Copy the example `.env` file and fill in your values:

```bash
cp .env.example .env
```

At minimum, set your Numista API credentials:

```env
NUMISTA_API_KEY=your_api_key_here
NUMISTA_CLIENT_ID=123456
```

> The database connection defaults (`localhost:5432`, user `coffer`, password `coffer`) match the Docker Compose dev setup, so no DB config changes are needed for local development.

### 4. Run the backend

```bash
cd coffer2
./gradlew bootRun
```

The API starts at `http://localhost:8080`. Liquibase will automatically create the database schema on first run.

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI spec:** http://localhost:8080/v3/api-docs

### 5. Run the frontend

```bash
cd coffer-ui
npm install
npm start
```

The app starts at `http://localhost:4200` with hot reload enabled.

## Running Tests

### Backend

```bash
cd coffer2
./gradlew test
```

Tests use [Testcontainers](https://www.testcontainers.org/) to spin up a temporary PostgreSQL instance, so Docker must be running.

### Frontend

```bash
cd coffer-ui
npm test
```

## Development Tips

- The backend uses **Liquibase** for database migrations. Schema changes go in `src/main/resources/db/changelog/`.
- The frontend uses **Angular 18** with standalone components, signals, and Tailwind CSS.
- The frontend proxies API requests to `http://localhost:8080` during development.
- **Coin images** are stored locally at `./data/images` by default (configurable via `STORAGE_BASE_PATH`).

## Submitting Changes

1. Fork the relevant repository
2. Create a feature branch from `master`
3. Make your changes
4. Ensure tests pass
5. Open a pull request with a clear description of what changed and why

## License

By contributing, you agree that your contributions will be licensed under the [AGPL-3.0 License](LICENSE).
