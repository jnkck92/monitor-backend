# Monitor Backend

![Build Status](https://img.shields.io/github/actions/workflow/status/jnkck92/monitor-backend/ci.yml?branch=develop&label=CI)
![Release](https://img.shields.io/github/v/release/jnkck92/monitor-backend)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green)
![License](https://img.shields.io/badge/license-MIT-blue)

A Spring Boot backend service that powers a fire department operations monitor. It polls the [Divera 24/7](https://www.divera247.com/) API for active alarms and provides REST endpoints to drive a real-time status display.

---

## Key Features

- **Real-time Alarm Polling** – Periodically fetches alarm data from the Divera 24/7 API
- **Dynamic Vehicle Ordering** – Automatically determines vehicle dispatch order based on configurable alarm rules
- **Hot-reloadable Configuration** – Instance configuration (vehicles, persons, rules) can be reloaded at runtime without restart
- **Multi-Platform Docker Images** – Built for `linux/amd64` and `linux/arm64` (Raspberry Pi compatible)
- **Dev Mode with Mock Alarms** – Simulated alarm triggers for local development and testing

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 4.1.1 | Application framework |
| Maven | 3.9+ | Build tool |
| Lombok | – | Boilerplate reduction |
| Jackson YAML | – | Configuration parsing |
| Docker | – | Containerization |
| GitHub Actions | – | CI/CD |

---

## Project Structure

```
src/main/java/de/jkueck/monitor/backend/
├── MonitorBackendApplication.java      # Entry point with scheduling enabled
├── client/                             # Divera API client abstraction
│   ├── DiveraClient.java              # Interface
│   ├── RealDiveraApiClient.java       # Production implementation
│   └── MockDiveraApiClient.java       # Dev/test mock
├── config/                             # Spring configuration & properties
│   ├── ConfigurationProperties.java
│   ├── DiveraProperties.java
│   └── ObjectMapperConfig.java
├── controller/                         # REST API endpoints
│   ├── ConfigurationController.java
│   ├── MonitorController.java
│   └── MockController.java            # Dev profile only
├── dto/                                # Data transfer objects
│   ├── configuration/
│   └── response/
└── service/                            # Business logic
    ├── ConfigurationService.java
    └── MonitorPollingService.java
```

---

## Prerequisites

- **Java 21** (JDK, e.g. Eclipse Temurin)
- **Maven 3.9+** or use the included Maven Wrapper (`./mvnw`)
- **Docker** (optional, for containerized deployment)

---

## Installation & Local Setup

### 1. Clone the repository

```bash
git clone https://github.com/jnkck92/monitor-backend.git
cd monitor-backend
```

### 2. Build the project

```bash
./mvnw clean package -DskipTests
```

### 3. Run locally (dev profile)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

The application starts on `http://localhost:8080`.

### 4. Run with Docker

```bash
docker build -t monitor-backend .
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -v ./local-config/instance-config.yaml:/app/config/instance-config.yaml:ro \
  monitor-backend
```

---

## Configuration

### `application.yaml` (Production defaults)

```yaml
spring:
  application:
    name: monitor-backend

server:
  port: 8080

divera:
  access-key: ${DIVERA_ACCESSKEY}
  base-url: ${DIVERA_BASE_URL}
  poll-interval-ms: ${DIVERA_POLL_INTERVAL_MS:10000}

configuration:
  path: ${CONFIGURATION_PATH:/app/config/instance-config.yaml}
```

### Environment Variables

| Variable | Required | Description |
|---|---|---|
| `DIVERA_ACCESSKEY` | ✅ | Divera 24/7 API access key |
| `DIVERA_BASE_URL` | ✅ | Divera API base URL |
| `DIVERA_POLL_INTERVAL_MS` | ❌ | Polling interval in ms (default: `10000`) |
| `CONFIGURATION_PATH` | ❌ | Path to instance config YAML (default: `/app/config/instance-config.yaml`) |

### Instance Configuration

The `instance-config.yaml` defines department-specific data:

- Department name
- Persons (leadership roles with Divera IDs)
- Vehicles (with RIC codes and Divera IDs)
- Status definitions and colors
- Alarm rule groups with keyword matching and vehicle dispatch order

See [`local-config/instance-config.yaml`](local-config/instance-config.yaml) for a full example.

---

## API Endpoints

### Production Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/monitor/status` | Returns current monitor state (alarm or standby) |
| `GET` | `/api/v1/configuration` | Returns the active instance configuration |
| `POST` | `/api/v1/configuration/reload` | Hot-reloads the instance configuration from disk |

### Dev-Only Endpoints (Profile: `dev`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/mock/alarm/on` | Simulates an active alarm |
| `POST` | `/mock/alarm/off` | Switches back to standby |

### Example Request

```bash
curl http://localhost:8080/api/v1/monitor/status
```

---

## Running Tests

```bash
# Run all tests
./mvnw verify

# Skip tests during build
./mvnw package -DskipTests
```

---

## Deployment (Docker Compose)

Example `docker-compose.yaml` for Raspberry Pi:

```yaml
services:
  monitor-backend:
    image: ghcr.io/jnkck92/monitor-backend:latest
    ports:
      - "8085:8080"
    environment:
      DIVERA_ACCESSKEY: your-api-key
      DIVERA_BASE_URL: https://www.divera247.com/api/v2
      DIVERA_POLL_INTERVAL_MS: 10000
      CONFIGURATION_PATH: /app/config/instance-config.yaml
    volumes:
      - ./local-config/instance-config.yaml:/app/config/instance-config.yaml:ro
    restart: unless-stopped
```

---

## Contributing

1. Create a feature branch from `develop`: `git checkout -b feature/my-feature develop`
2. Make your changes and commit
3. Push and open a Pull Request against `develop`
4. After review and merge, a release is created via the [Release Flow](RELEASE.md)

---

## License

This project is licensed under the MIT License.
