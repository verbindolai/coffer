<p align="center">
  <img src="https://raw.githubusercontent.com/verbindolai/coffer-ui/refs/heads/master/public/favicon.svg" width="80" alt="Coffer Logo">
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
| **Framework** | Spring Boot |
| **Language** | Kotlin |
| **Database** | PostgreSQL |
| **Migrations** | Liquibase |
| **API Docs** | OpenAPI / Swagger UI |
| **Build** | Gradle (Kotlin DSL) |
| **Runtime** | Java |

## Getting Started

### Prerequisites

- Java 21+
- PostgreSQL 16+
- Gradle 8+ (or use the wrapper)

### Development Setup

```bash
# Clone the repository
git clone https://github.com/verbindolai/coffer2.git
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

For a complete deployment with frontend and database, see [coffer-deploy](https://github.com/verbindolai/coffer-deploy).

## API Documentation

Once running, explore the API:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/v3/api-docs
- **OpenAPI YAML:** http://localhost:8080/v3/api-docs.yaml

## Configuration

Key environment variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_DATASOURCE_URL` | - | PostgreSQL connection URL |
| `SPRING_DATASOURCE_USERNAME` | - | Database username |
| `SPRING_DATASOURCE_PASSWORD` | - | Database password |
| `NUMISTA_API_KEY` | - | Numista API key for catalog features |
| `COFFER_IMAGES_PATH` | `./data/images` | Path for coin image storage |


## Related Projects

- [coffer-ui](https://github.com/verbindolai/coffer-ui) - Angular frontend
- [coffer-deploy](https://github.com/verbindolai/coffer-deploy) - Docker deployment

## License

AGPL-3.0 License - See [LICENSE](LICENSE) for details.
