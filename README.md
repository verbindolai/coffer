<p align="center">
  <img src="https://raw.githubusercontent.com/YOUR_USERNAME/coffer2-ui/main/public/favicon.svg" width="80" alt="Coffer Logo">
</p>

<h1 align="center">Coffer Backend</h1>

<p align="center">
  <strong>REST API for the Coffer Coin Collection Manager</strong>
</p>

<p align="center">
  <a href="#features">Features</a> •
  <a href="#tech-stack">Tech Stack</a> •
  <a href="#getting-started">Getting Started</a> •
  <a href="#api-documentation">API Docs</a> •
  <a href="#configuration">Configuration</a>
</p>

---

## Features

- **Coin Management** - Full CRUD for your collection with detailed metadata (year, country, denomination, grade, rarity)
- **Metal Valuations** - Real-time precious metal prices (gold, silver, platinum) with automatic updates
- **Portfolio Analytics** - Track total value, metal composition, and historical performance
- **Numista Integration** - Search the Numista catalog for coin details and market prices
- **Image Storage** - Upload and manage coin images with automatic fetching from catalogs
- **Scheduled Tasks** - Automatic metal quote updates and portfolio snapshots

## Tech Stack

| Layer | Technology |
|-------|------------|
| **Framework** | Spring Boot 4.0 |
| **Language** | Kotlin 2.2 |
| **Database** | PostgreSQL 16 |
| **Migrations** | Liquibase |
| **API Docs** | OpenAPI 3 / Swagger UI |
| **Build** | Gradle (Kotlin DSL) |
| **Runtime** | Java 21 |

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 16+
- Gradle 8+ (or use the wrapper)

### Development Setup

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/coffer2.git
cd coffer2

# Configure database connection
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/coffer
export SPRING_DATASOURCE_USERNAME=coffer
export SPRING_DATASOURCE_PASSWORD=yourpassword

# Run the application
./gradlew bootRun
```

The API will be available at `http://localhost:8080`.

### Using Docker

For a complete deployment with frontend and database, see [coffer-deploy](https://github.com/YOUR_USERNAME/coffer-deploy).

## API Documentation

Once running, explore the API:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

### Core Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/v1/coins` | List coins (paginated, filterable) |
| `POST /api/v1/coins` | Create a new coin |
| `GET /api/v1/coins/{id}` | Get coin details |
| `PUT /api/v1/coins/{id}` | Update a coin |
| `DELETE /api/v1/coins/{id}` | Delete a coin |
| `GET /api/v1/coins/{id}/valuation` | Get coin valuation history |
| `GET /api/v1/portfolio/valuation` | Get portfolio valuation |
| `GET /api/v1/catalog/search` | Search Numista catalog |

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | - | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | - | Database username |
| `SPRING_DATASOURCE_PASSWORD` | - | Database password |
| `NUMISTA_API_KEY` | - | Numista API key for catalog features |
| `COFFER_IMAGES_PATH` | `./data/images` | Path for coin image storage |

## Project Structure

```
src/main/kotlin/org/coffer/coffer2/
├── api/            # REST controllers
├── application/    # Use cases and services
├── domain/         # Business logic and entities
├── repository/     # Data access (JPA repositories)
├── remote/         # External API clients (Numista, metal prices)
└── schedule/       # Scheduled tasks (quotes, snapshots)
```

## Related Projects

- [coffer2-ui](https://github.com/YOUR_USERNAME/coffer2-ui) - Angular frontend
- [coffer-deploy](https://github.com/YOUR_USERNAME/coffer-deploy) - Docker deployment

## License

MIT License - See [LICENSE](LICENSE) for details.
