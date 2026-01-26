super# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build project
./gradlew build

# Run locally (local profile has hardcoded credentials - no env vars needed)
./gradlew bootRun --args='--spring.profiles.active=local'

# Run with dev profile (requires environment variables)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Run tests (uses test profile automatically)
./gradlew test

# Run a single test class
./gradlew test --tests "com.firstaid.unit.service.DrugServiceTest"

# Run a single test method
./gradlew test --tests "com.firstaid.unit.service.DrugServiceTest.shouldReturnDrug"

# Generate coverage report (outputs to build/reports/jacoco/test/html/)
./gradlew jacocoTestReport
```

### Docker Commands

```bash
# Full rebuild and start
docker-compose down -v --remove-orphans && ./gradlew build && docker-compose build --no-cache && docker-compose up

# Start postgres only (for local development)
docker-compose up -d postgres

# Start dev postgres (separate compose file)
docker compose -f docker-compose-dev.yml up -d
```

## Architecture Overview

Spring Boot 4.0.1 application with Java 25, following a layered architecture with multi-tenant data isolation.

### Package Structure

```
com.firstaid/
├── controller/          # REST endpoints (@RestController)
│   ├── drug/           # DrugController - main CRUD and search
│   ├── auth/           # AuthController - login, register, tokens, /me
│   ├── admin/          # AdminController - user management
│   └── handler/        # GlobalExceptionHandler - centralized error handling
├── service/            # Business logic (@Service)
├── domain/exception/   # Custom exceptions (DrugNotFoundException, AccountLockedException, etc.)
└── infrastructure/
    ├── database/
    │   ├── entity/     # JPA entities (DrugEntity, UserEntity, RoleEntity)
    │   ├── repository/ # Spring Data JPA repositories
    │   └── mapper/     # MapStruct mappers (entity ↔ DTO)
    ├── security/       # JWT auth, SecurityConfiguration, RateLimitingFilter
    ├── configuration/  # AsyncConfig, CacheConfig, OpenApiConfig
    ├── validation/     # Custom validators (@ValidPassword, @ValueOfEnum, etc.)
    ├── cache/          # UserAwareCacheKeyGenerator (multi-tenant cache keys)
    ├── email/          # EmailService (async SMTP with retry)
    └── pdf/            # PdfExportService (OpenPDF library)
```

### Key Architectural Patterns

**Multi-Tenancy**: Each user's data is isolated via `user_id` foreign key. `CurrentUserService` extracts the authenticated user from the security context. All repository queries filter by user ID. `UserAwareCacheKeyGenerator` ensures cache keys include user ID.

**JWT Authentication**: Access token (1h) + refresh token (24h) with rotation. `JwtTokenProvider` handles token generation/validation. `JwtAuthenticationFilter` validates tokens on each request. Stateless sessions. Refresh token rotation generates new token on each refresh.

**Caching**: Caffeine-backed Spring Cache with caches: `drugById`, `drugsSearch`, `drugStatistics`. All mutating operations use `@CacheEvict`. Cache keys are user-aware for tenant isolation.

**Database Migrations**: Flyway migrations in `src/main/resources/db/migration/`. Schema versioned V1.0 through V6.0.

**Async Processing**: `@EnableAsync` with dedicated `emailTaskExecutor` thread pool. Email sending is async with Resilience4j retry (3 attempts, exponential backoff).

## Security Features

### Rate Limiting
- Bucket4j-based filter on auth endpoints (`/api/auth/login`, `/register`, `/forgot-password`, `/reset-password`)
- Default: 20 requests/minute per IP
- Config: `app.rate-limit.enabled`, `app.rate-limit.requests-per-minute`

### Account Lockout
- 5 failed login attempts → 15 minute lockout
- Tracked in `UserEntity.failedLoginAttempts` and `lockedUntil`
- Auto-reset on successful login

### Password Complexity
- `@ValidPassword` annotation requires: min 8 chars, uppercase, lowercase, digit, special char
- Applied to: `RegisterRequest`, `ResetPasswordRequest`, `ChangePasswordRequest`

### CORS
- Configured in `SecurityConfiguration.corsConfigurationSource()`
- Config: `app.cors.allowed-origins` (comma-separated, default: `http://localhost:3000,http://localhost:5173`)

## API Endpoints

### Auth Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login (returns JWT tokens)
- `POST /api/auth/refresh` - Refresh tokens (with rotation)
- `GET /api/auth/me` - Get current user profile (requires auth)
- `POST /api/auth/forgot-password` - Initiate password reset
- `POST /api/auth/reset-password` - Complete password reset
- `POST /api/auth/change-password` - Change password (requires auth)
- `DELETE /api/auth/account` - Delete account (requires auth)

### Health & Monitoring
- `GET /actuator/health` - Health check (public)
- `GET /actuator/info` - App info (public)
- `GET /actuator/metrics` - Metrics (requires ADMIN role)

## Testing Structure

- **Integration tests** (`src/test/java/.../integration/`): Use Testcontainers with PostgreSQL. Extend `AbstractIntegrationTest` for shared context.
- **Slice tests** (`src/test/java/.../slice/controller/`): Controller-layer validation tests without DB.
- **Unit tests** (`src/test/java/.../unit/service/`): Pure business logic tests with mocked dependencies.
- **E2E tests** (`src/test/java/.../integration/e2e/`): Full workflow tests.

Test utilities in `src/test/java/.../util/` (e.g., `DrugRequestDtoBuilder`).

JaCoCo requires 80% line coverage minimum.

## Configuration Profiles

| Profile | Port | Use Case |
|---------|------|----------|
| `local` | 8082 | IDE development, hardcoded credentials |
| `dev` | 8081 | Local with Docker postgres, requires env vars |
| `docker` | 8081 | Full Docker Compose deployment |
| `prod` | 8080 | Production (Swagger disabled, stricter rate limits) |
| `test` | - | Automated tests (auto-activated) |

Environment variables needed for non-local profiles: `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` (optional).

## Key Configuration Properties

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}
    access-token-expiration-ms: 3600000    # 1 hour
    refresh-token-expiration-ms: 86400000  # 24 hours
  cors:
    allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
  rate-limit:
    enabled: true
    requests-per-minute: 20
  admin:
    email: ${ADMIN_EMAIL}  # Auto-grants ADMIN role on registration

resilience4j:
  retry:
    instances:
      emailService:
        maxAttempts: 3
        waitDuration: 2s
        enableExponentialBackoff: true
```

## Key Files

- `SecurityConfiguration.java`: Spring Security setup with JWT filter, CORS, rate limiting
- `RateLimitingFilter.java`: Bucket4j-based rate limiting for auth endpoints
- `JwtTokenProvider.java`: JWT token generation and validation
- `AuthService.java`: Authentication logic with account lockout
- `DrugService.java`: Main business logic for drug CRUD and search
- `EmailService.java`: Async email sending with Resilience4j retry
- `GlobalExceptionHandler.java`: Centralized exception handling
- `ValidPassword.java` + `ValidPasswordValidator.java`: Password complexity validation
- `AsyncConfig.java`: Thread pool configuration for async operations
