# Monitor Backend

![Build Status](https://img.shields.io/github/actions/workflow/status/jnkck92/monitor-backend/ci.yml?branch=develop&label=CI)
![Release](https://img.shields.io/github/v/release/jnkck92/monitor-backend)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-green)
![License](https://img.shields.io/badge/license-MIT-blue)

A Spring Boot backend service that powers a fire department operations monitor. It polls the [Divera 24/7](https://www.divera247.com/) API for active alarms and provides REST endpoints to drive a real-time status display.

---

## Key Features

- **Real-time Alarm Polling** – Periodically fetches alarm and vehicle status data from the Divera 24/7 API
- **Intelligent Keyword Matching** – Configurable alarm rules with multiple match modes (CONTAINS, EXACT, STARTS_WITH, REGEX) and longest-match priority
- **Dynamic Vehicle Ordering** – Automatically determines vehicle dispatch order based on alarm type
- **Hot-reloadable Configuration** – Instance configuration can be reloaded at runtime without restart
- **Health Monitoring** – Spring Actuator health endpoint with custom Divera connection indicator
- **Metrics** – Micrometer metrics for poll duration, error rates and state changes
- **Startup Validation** – Validates Divera API connection on startup (live profile)
- **Graceful Shutdown** – Clean shutdown with configurable timeout
- **OpenAPI / Swagger** – Interactive API documentation
- **Multi-Platform Docker Images** – Built for `linux/amd64` and `linux/arm64` (Raspberry Pi compatible)
- **Dev Mode with Mock Alarms** – Simulated alarm triggers for local development and testing

---

## Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Runtime |
| Spring Boot | 4.1.1 | Application framework |
| Spring Actuator | – | Health checks & metrics |
| Micrometer | – | Application metrics |
| springdoc-openapi | 2.8.8 | Swagger UI & OpenAPI docs |
| Maven | 3.9+ | Build tool |
| Lombok | – | Boilerplate reduction |
| Jackson YAML | – | Configuration parsing |
| WireMock | 3.13.0 | API client integration tests |
| Docker | – | Containerization |
| GitHub Actions | – | CI/CD |

---

## Project Structure

```
src/main/java/de/jkueck/monitor/backend/
├── MonitorBackendApplication.java        # Entry point with scheduling enabled
├── client/                               # Divera API client abstraction
│   ├── DiveraClient.java                 # Interface
│   ├── RealDiveraApiClient.java          # Production impl (live profile, with timeouts)
│   └── MockDiveraApiClient.java          # Dev/test mock (dev profile)
├── config/                               # Spring configuration & properties
│   ├── ClockConfig.java                  # Clock bean for testability
│   ├── ConfigurationProperties.java      # Instance config file path
│   ├── DiveraConnectionValidator.java    # Startup API key validation (live profile)
│   ├── DiveraHealthIndicator.java        # Custom health check for /actuator/health
│   ├── DiveraProperties.java             # Divera API settings
│   ├── ObjectMapperConfig.java           # JSON & YAML ObjectMapper beans
│   └── OpenApiConfig.java               # Swagger/OpenAPI metadata
├── controller/                           # REST API endpoints
│   ├── ConfigurationController.java      # Config read & reload
│   ├── GlobalExceptionHandler.java       # RFC 9457 ProblemDetail error responses
│   ├── MockController.java              # Dev profile only – simulate alarms
│   └── MonitorController.java           # Main status endpoint
├── dto/                                  # Data transfer objects
│   ├── configuration/                    # Instance config model
│   │   ├── Configuration.java
│   │   ├── Rule.java
│   │   ├── RuleGroup.java
│   │   ├── Status.java
│   │   └── Unit.java
│   └── response/                         # API response model
│       ├── AlarmWebResponse.java
│       ├── MonitorWebResponse.java
│       ├── RadioStatusWebResponse.java
│       ├── UnitWebResponse.java
│       └── divera/                       # Divera API response model
│           ├── AlarmResponse.java
│           ├── DiveraResponse.java
│           ├── VehicleStatus.java
│           └── VehicleStatusGroupResponse.java
└── service/                              # Business logic
    ├── ActiveAlarmResolver.java          # Finds active (non-closed) alarm
    ├── ConfigurationService.java         # Loads & caches instance config from YAML
    ├── DiveraResponseLogger.java         # Logs alarm transitions to file
    ├── KeywordMatcher.java               # Alarm keyword matching with multiple modes
    ├── MatchMode.java                    # Enum: CONTAINS, EXACT, STARTS_WITH, REGEX
    ├── MonitorMode.java                  # Enum: STANDBY, ALARM
    ├── MonitorPollingService.java         # Orchestrates polling cycle with metrics
    ├── MonitorStateBuilder.java          # Builds MonitorWebResponse from raw data
    ├── UnitStatusEnricher.java           # Maps live FMS status to display DTOs
    └── VehicleOrderBuilder.java          # Orders vehicles by alarm rules
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

### Application Configuration

```yaml
spring:
  application:
    name: monitor-backend
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  port: 8080
  shutdown: graceful

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
| `SPRING_PROFILES_ACTIVE` | ✅ | Active profile (`dev` or `live`) |
| `DIVERA_ACCESSKEY` | ✅ | Divera 24/7 API access key |
| `DIVERA_BASE_URL` | ✅ | Divera API base URL |
| `DIVERA_POLL_INTERVAL_MS` | ❌ | Polling interval in ms (default: `10000`) |
| `CONFIGURATION_PATH` | ❌ | Path to instance config YAML (default: `/app/config/instance-config.yaml`) |

### Instance Configuration

The `instance-config.yaml` defines all department-specific settings:

| Section | Description |
|---|---|
| `departmentName` | Display name of the fire department |
| `commandContact` | Contact info for command (e.g. ELW call sign) |
| `persons` | Leadership personnel with Divera IDs and RIC codes |
| `vehicles` | Vehicles/units with Divera IDs and RIC codes |
| `defaultOrder` | Default vehicle display order (list of vehicle IDs) |
| `statuses` | FMS status definitions with labels and colors (keys: `"1"` – `"6"`) |
| `ruleGroups` | Alarm categories (e.g. Brand, Hilfeleistung, CBRN, Unterstützung) |
| `ruleGroups[].rules` | Individual alarm rules with keyword matching and vehicle dispatch order |
| `rules[].matchMode` | Matching strategy: `CONTAINS` (default), `EXACT`, `STARTS_WITH`, `REGEX` |
| `rules[].hint` | Optional tactical hint displayed during alarm |
| `rules[].remainingOrder` | Optional custom order for non-alerted vehicles |

📄 **Full schema documentation:** [`docs/instance-config-schema.md`](docs/instance-config-schema.md)
📝 **Example configuration:** [`local-config/instance-config.yaml`](local-config/instance-config.yaml)

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

### Monitoring Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/actuator/health` | Health status with Divera connection indicator |
| `GET` | `/actuator/info` | Application info |
| `GET` | `/actuator/metrics` | Available metrics |

### Documentation

| Endpoint | Description |
|---|---|
| `/swagger-ui.html` | Interactive Swagger UI |
| `/api-docs` | OpenAPI 3.0 JSON specification |

### Custom Metrics

| Metric | Description |
|---|---|
| `monitor.poll.duration` | Duration of each Divera poll cycle |
| `monitor.poll.errors` | Number of failed poll attempts |
| `monitor.state.changes` | Number of state transitions (STANDBY ↔ ALARM) |

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
      SPRING_PROFILES_ACTIVE: live
      DIVERA_ACCESSKEY: your-api-key
      DIVERA_BASE_URL: https://www.divera247.com/api
      DIVERA_POLL_INTERVAL_MS: 10000
      CONFIGURATION_PATH: /app/config/instance-config.yaml
    volumes:
      - ./local-config/instance-config.yaml:/app/config/instance-config.yaml:ro
    restart: unless-stopped
    stop_grace_period: 35s
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