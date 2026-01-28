<pre>
######## #### ########   ######  ########       ###    #### ########     ##    ## #### ######## 
##        ##  ##     ## ##    ##    ##         ## ##    ##  ##     ##    ##   ##   ##     ##    
##        ##  ##     ## ##          ##        ##   ##   ##  ##     ##    ##  ##    ##     ##    
######    ##  ########   ######     ##       ##     ##  ##  ##     ##    #####     ##     ##    
##        ##  ##   ##         ##    ##       #########  ##  ##     ##    ##  ##    ##     ##    
##        ##  ##    ##  ##    ##    ##       ##     ##  ##  ##     ##    ##   ##   ##     ##    
##       #### ##     ##  ######     ##       ##     ## #### ########     ##    ## ####    ## 
</pre>


![Java](https://img.shields.io/badge/Java-25-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-green)
![JSpecify](https://img.shields.io/badge/Null--Safety-JSpecify-orange)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

# First Aid Kit Application

A lightweight Java-based application for managing a database of drugs. It supports creating, updating, deleting, and
querying drug records, along with automated email alerts for medications nearing expiration. An optional PDF export
feature is also available for listing current drugs.

## 🌟 Key Features

- **CRUD Operations**:
    - Add new drugs to the system.
    - Update existing drug details.
    - Delete drugs from the database.
    - Retrieve drug details with advanced filtering (e.g., by name, form, or expiration date).

- **Drug Expiration Alerts**:
    - Automatically sends email alerts for drugs approaching their expiration date (1 month prior).

- **Statistics**:
    - Comprehensive dashboard data: total drug count, expired vs. active medications, and history of sent alerts.

- **Multi-Tenancy & Data Isolation**:
    - Each user manages a strictly isolated drug collection.
    - Automatic data filtering based on the authenticated user's identity.
    - Enhanced security with **User-Aware Caching** to prevent cross-user data leakage.

- **Authentication & Security**:
    - Secure **JWT-based** authentication (Access + Refresh tokens).
    - Registration and login via email & password.
    - Role-based access control (USER, ADMIN).
    - Automated welcome emails upon successful registration.
    - Strict **Null-Safety** implementation using **JSpecify** across core infrastructure.

- **Account Management**:
    - Permanent account deletion with all associated data.
    - Secure password recovery (Forgot Password) via email tokens.
    - In-app password change for authenticated users.

## 📦 API Endpoints

Full documentation is available via **Swagger UI**. The access URL depends on your environment:

- **Docker**: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- **Local (IDE)**: [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

### 🔹 Drug Management (CRUD)

- **`GET /api/drugs/search`** — Unified search endpoint with filtering, sorting, and pagination.
- **`GET /api/drugs/{id}`** — Returns a specific drug by ID (`DrugResponse`).
- **`POST /api/drugs`** — Adds a new drug (`DrugRequest`).
- **`PUT /api/drugs/{id}`** — Updates an existing drug by its ID.
- **`DELETE /api/drugs/{id}`** — Removes a drug from the database.

---

### 🔐 Authentication

- **`POST /api/auth/register`** — Registers a new user account.
- **`POST /api/auth/login`** — Authenticates user and returns JWT Access & Refresh tokens.
- **`POST /api/auth/refresh`** — Refreshes the access token using a valid refresh token.

> **Note:** Protected endpoints require an `Authorization: Bearer <accessToken>` header.

---

### 👤 Account Management

- **`DELETE /api/auth/account`** — Permanently deletes the authenticated user's account and all associated data.
- **`POST /api/auth/forgot-password`** — Initiates password recovery via email.
- **`POST /api/auth/reset-password`** — Resets the password using a valid reset token.
- **`GET /api/auth/validate-reset-token?token=...`** — Validates if a password reset token is still valid.
- **`POST /api/auth/change-password`** — Changes the password for the authenticated user (requires auth).

---

### 🔎 Filtering & Search

**`GET /api/drugs/search`**
**Query parameters:**

- `name` — substring match (case-insensitive).
- `form` — enum value (e.g., `PILL`, `GEL`). Now **case-insensitive** at the API level.
- `expired` — `true|false`.
- `expirationUntilYear` / `expirationUntilMonth` — e.g., `2026` / `10`.
- `page` / `size` / `sort` — standard pagination and sorting (e.g., `drugName,asc`).

**Examples:**

- `/api/drugs/search?name=ibu&form=gel`
- `/api/drugs/search?expired=true&sort=expirationDate,asc&size=100`

---

### 📚 Supplementary & Tools

- **`GET /api/drugs/forms/dictionary`** — Returns a dictionary of drug forms and their human-readable labels.
- **`GET /api/drugs/statistics`** — Returns data on total, expired, and active medications.
- **`GET /api/drugs/export/pdf`** — Exports the current drug list to PDF (max size: 100).
- **`GET /api/drugs/alert`** — Manually triggers expiry alert emails for drugs expiring this month.

### 💾 Performance & Caching

To improve performance, the application uses Spring Cache backed by Caffeine:

- Caches: `drugById`, `drugsSearch`, `drugStatistics`.
- TTL/size configured via `spring.cache.caffeine.spec` in `application.yml`.
- Mutating operations (POST/PUT/DELETE) evict relevant entries to keep reads consistent.

## Prerequisites

Before running this application, ensure you have the following installed:

- Java 25
- Gradle (build system used in this project)
- PostgreSQL (or any compatible relational database)
- Docker (optional, for containerized setup)

## 🚀 Setup & Run the Application

> **Recommended:** To run the full stack (Backend + Frontend + Database) with a single command,
> use the [First Aid Kit Hub](https://github.com/NextStepProDev/first-aid-kit-manager-hub) repository.
> The instructions below are for running the API standalone.

### Port Reference

| Environment | Port | Profile | Use Case |
|-------------|------|---------|----------|
| Local (IDE) | 8082 | `dev` | Development in IntelliJ/VS Code |
| Docker (standalone) | 8081 | `docker` | docker-compose from this repo |
| Hub (full stack) | 8080 | `docker` | Deployment via Hub repository |

There are two ways to run the application: locally via IntelliJ/Gradle or using Docker.

### ✅ Option 1: Run Locally (IntelliJ or Terminal)

1. Clone the repository:
   ```bash
   git clone https://github.com/NextStepProDev/first_aid_kit.git
   cd first_aid_kit
   ```

2. Make sure you have PostgreSQL running (either locally or via Docker) and that the credentials match the
   configuration.

   If running the application locally (outside Docker), you must define the required environment variables manually in
   your system or via IntelliJ:

    - `POSTGRES_USER`
    - `POSTGRES_PASSWORD`
    - `POSTGRES_DB`
    - `MAIL_USERNAME`
    - `MAIL_PASSWORD`
    - `JWT_SECRET`

   You can also find example values in the `.env.example` file.

   **Alternative: Use `dev` profile (recommended for IntelliJ)**

   The `dev` profile (`application-dev.yml`) uses `spring.config.import` to automatically load database and mail
   credentials from your local `.env` file, so you don't need to set environment variables manually.

   In IntelliJ:
    - Go to **Run → Edit Configurations**
    - Set **Active profiles:** `dev`
    - Or add VM option: `-Dspring.profiles.active=dev`

3. Run the application:
   ```bash
   # With dev profile (requires env variables)
   ./gradlew bootRun --args='--spring.profiles.active=dev'

   ```

4. Access Swagger UI:
   [http://localhost:8082/swagger-ui/index.html](http://localhost:8082/swagger-ui/index.html)

---

### 🐳 Option 2: Run with Docker (Recommended)

This project includes Docker configuration for running PostgreSQL and pgAdmin. Follow these steps to set it up locally.

1. Environment Configuration

Create a `.env` file in the project root (next to `docker-compose.yml`). Use the `.env.example` file as a template:

```dotenv
# 👉 This is an example .env file.

# =======================
# PostgreSQL Configuration
# =======================
POSTGRES_DB=first_aid_kit                  # Name of the database
POSTGRES_USER=exampleUser                  # Database username
POSTGRES_PASSWORD=examplePassword          # Database password
POSTGRES_PORT=5432                         # Port PostgreSQL listens on

# =======================
# pgAdmin Configuration
# =======================
PGADMIN_DEFAULT_EMAIL=example@example.com   # Login email for pgAdmin
PGADMIN_DEFAULT_PASSWORD=admin1234          # Login password for pgAdmin
PGADMIN_PORT=5050                           # Local port to access pgAdmin UI

# =======================
# Mail Configuration (Gmail)
# =======================
MAIL_USERNAME=youremail@gmail.com           # Your Gmail address
MAIL_PASSWORD=your_app_password             # App password or SMTP password

# =======================
# JWT Secret Configuration
# =======================
# JWT Secret (min 256 bits / 32 characters)
JWT_SECRET=DockerSecretKeyForJWTTokenExampleMustBeAtLeast256BitsLong
```

> 🔒 **Do not commit the `.env` file to version control** — it's excluded via `.gitignore`.

2. Build and start the containers:

```bash
# Optional cleanup step
# ⚠️ Warning: This will remove all containers and their named volumes (e.g., database data)
docker-compose down -v --remove-orphans 

# 🔒 Safer alternative (volumes will be preserved)
docker-compose down --remove-orphans

# Build the project
./gradlew build

# Rebuild containers from scratch (skips Docker cache)
docker-compose build --no-cache

# Start containers
docker-compose up
```

> 📌 Make sure Docker is running before using this method.

### 3. Access the application:

- Swagger UI: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- pgAdmin: [http://localhost:5050](http://localhost:5050) (or the value of `PGADMIN_PORT` from `.env`)

### 4. Persistent database data

PostgreSQL data is stored in a Docker volume:

```yaml
volumes:
  postgres_data:
    name: first_aid_kit_postgres_data
```

This means your database data will persist even if you remove the container.  
To view or manage volumes:

```bash
docker volume ls
docker volume inspect first_aid_kit_postgres_data
```

## 🔐 Security & Best Practices

- Never commit `.env` – it's ignored by `.gitignore`
- Use `.env.example` to share required configuration without secrets
- Environment variables are injected into the application via `docker-compose.yml`
- Mail credentials should use application-specific passwords (not your Gmail login)
- Use strong passwords for database and email accounts

## 📚 API Documentation

Swagger UI: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

## Technologies Used

- Java 25
- Spring Boot 4.0.1
- Spring Web / Validation / Security / Data JPA
- Spring Cache with Caffeine
- JWT (JSON Web Tokens) for authentication
- MapStruct
- PostgreSQL
- Flyway
- Docker & Docker Compose
- OpenAPI / Swagger
- Testcontainers (PostgreSQL integration tests)

## 📦 Project Structure

The project follows a modular and domain-driven architecture to ensure clear separation of concerns:

- **`controller/`** – REST endpoints organized by feature domains:
    - `admin/`, `alert/`, `auth/`, `drug/`.
- **`dto/`** – Data Transfer Objects (Requests/Responses) strictly categorized:
    - `admin/`, `auth/`, `drug/`, `error/`.
- **`domain/`** – Core business entities and domain-specific exceptions (e.g., `exception/`).
- **`infrastructure/`** – Technical cross-cutting concerns:
    - `bootstrap/` – Initial sample data loading.
    - `cache/` – User-aware caching key generation.
    - `configuration/` – Global Spring Boot & library settings.
    - `database/`, `email/`, `pdf/`, `security/`, `validation/`, `util/`.
- **`service/`** – Business logic orchestration and service layer.
- **`integration/`** – Integration tests leveraging Testcontainers.

## ✅ Tests

This project includes **unit**, **slice (web)**, **integration**, and **E2E** tests using JUnit 5, Spring Test, and
Testcontainers.

Run all tests:

```bash
./gradlew test
```

## Example API Usage

### Create a new drug

```http
POST /api/drugs
Content-Type: application/json
```

```json
{
  "name": "Ibuprofen",
  "form": "PILLS",
  "expirationYear": 2025,
  "expirationMonth": 5,
  "description": "Painkiller for fever and inflammation"
}
```

### Search: expiring drugs until a specific month

```http
GET /api/drugs/search?expirationUntilYear=2025&expirationUntilMonth=10&sort=expirationDate,asc&size=100
```

_Response example (paginated):_

```json
{
  "content": [
    {
      "drugId": 1,
      "drugName": "Paracetamol",
      "drugForm": "PILL",
      "expirationDate": "2025-10-31T00:00:00Z",
      "drugDescription": "Painkiller"
    },
    {
      "drugId": 2,
      "drugName": "Ibuprofen",
      "drugForm": "GEL",
      "expirationDate": "2025-10-15T00:00:00Z",
      "drugDescription": "Used to treat pain and fever"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 100
  },
  "totalElements": 2,
  "totalPages": 1
}
```

### Search: by name and form

```http
GET /api/drugs/search?name=ibu&form=GEL
```

## 📦 Versioning

This project uses [semantic versioning](https://semver.org/) — format: `MAJOR.MINOR.PATCH`.

- Current version: **`2.1.0-SNAPSHOT`**

You can browse release history [here](https://github.com/NextStepProDev/first_aid_kit/releases).

### License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

Made with ❤️ during my journey to become a professional Java
Developer – [Mateusz Nawratek](https://www.linkedin.com/in/mateusz-nawratek-909752356)