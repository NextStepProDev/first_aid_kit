<pre>
######## #### ########   ######  ########       ###    #### ########     ##    ## #### ######## 
##        ##  ##     ## ##    ##    ##         ## ##    ##  ##     ##    ##   ##   ##     ##    
##        ##  ##     ## ##          ##        ##   ##   ##  ##     ##    ##  ##    ##     ##    
######    ##  ########   ######     ##       ##     ##  ##  ##     ##    #####     ##     ##    
##        ##  ##   ##         ##    ##       #########  ##  ##     ##    ##  ##    ##     ##    
##        ##  ##    ##  ##    ##    ##       ##     ##  ##  ##     ##    ##   ##   ##     ##    
##       #### ##     ##  ######     ##       ##     ## #### ########     ##    ## ####    ## 
</pre>


![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-green)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)

# First Aid Kit Application

A lightweight Java-based application for managing a database of drugs. It supports creating, updating, deleting, and 
querying drug records, along with automated email alerts for medications nearing expiration. An optional PDF export 
feature is also available for listing current drugs.

## Key Features

- **CRUD Operations**: 
  - Add new drugs to the system.
  - Update existing drug details.
  - Delete drugs from the database.
  - Retrieve drug details by various filters (e.g., by expiration date).
  
- **Drug Expiration Alerts**:
  - Automatically send email alerts for drugs that are close to their expiration date (1 month before).
  
- **Statistics**:
  - Get statistics on the total number of drugs, expired drugs, active drugs, and alerts sent.

- **Multi-Tenancy**:
  - Each user has their own isolated drug collection.
  - Users can only view and manage their own medications.
  - Data is automatically filtered by the authenticated user.

- **Authentication & Security**:
  - JWT-based authentication (access + refresh tokens).
  - User registration and login via **email + password**.
  - Role-based access control (USER, ADMIN).
  - Welcome email sent upon successful registration.

- **Account Management**:
  - Delete account permanently.
  - Password recovery via email (forgot password).
  - Password change for authenticated users.

## 📦 API Endpoints

Full documentation available via [Swagger UI](http://localhost:8081/swagger-ui/index.html).

### 🔹 CRUD Operations
- **`GET /api/drugs/search`**  
  _Returns a list of all drugs in the database._

- **`GET /api/drugs/{id}`**  
  _Returns a drug by its ID._

- **`POST /api/drugs`**  
  _Adds a new drug to the database._

- **`PUT /api/drugs/{id}`**  
  _Updates an existing drug by its ID._

- **`DELETE /api/drugs/{id}`**
  _Deletes a drug by its ID._

---

### 🔐 Authentication

- **`POST /api/auth/register`**
  _Registers a new user account._
  ```json
  {
    "username": "john_doe",
    "email": "john@example.com",
    "password": "securePassword123",
    "name": "John Doe"
  }
  ```

- **`POST /api/auth/login`**
  _Authenticates user with email and password. Returns JWT tokens._
  ```json
  {
    "email": "john@example.com",
    "password": "securePassword123"
  }
  ```
  _Response:_
  ```json
  {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "username": "john_doe",
    "email": "john@example.com"
  }
  ```

- **`POST /api/auth/refresh`**
  _Refreshes the access token using a valid refresh token._
  ```json
  {
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```

> **Note:** Protected endpoints require `Authorization: Bearer <accessToken>` header.

---

### 👤 Account Management

- **`DELETE /api/auth/account`**
  _Permanently deletes the authenticated user's account and all associated data._

- **`POST /api/auth/forgot-password`**
  _Initiates password recovery via email._
  ```json
  {
    "email": "john@example.com"
  }
  ```

- **`POST /api/auth/reset-password`**
  _Resets the password using a valid reset token._
  ```json
  {
    "token": "reset-token-from-email",
    "newPassword": "newSecurePassword123",
    "confirmPassword": "newSecurePassword123"
  }
  ```

- **`GET /api/auth/validate-reset-token?token=...`**
  _Validates if a password reset token is still valid._

- **`POST /api/auth/change-password`** _(requires authentication)_
  _Changes the password for the authenticated user._
  ```json
  {
    "currentPassword": "oldPassword123",
    "newPassword": "newSecurePassword123",
    "confirmPassword": "newSecurePassword123"
  }
  ```

---

### 🔎 Filtering & Search
- **`GET /api/drugs/search`**  
  _Unified search endpoint with filtering, sorting, and pagination._
  **Query params:**
  - `name` — substring match (case-insensitive)
  - `form` — enum value (e.g., `PILL`, `GEL`, `SYRUP`, ...)
  - `expired` — `true|false`
  - `expirationUntilYear` — e.g., `2025`
  - `expirationUntilMonth` — `1..12`
  - `page` — default `0`
  - `size` — default `20`, **max `100`**
  - `sort` — e.g., `drugName,asc` or `expirationDate,desc`

_Examples:_
- `/api/drugs/search?name=ibu&form=GEL`
- `/api/drugs/search?expired=true&sort=expirationDate,asc&size=100`
- `/api/drugs/search?expirationUntilYear=2025&expirationUntilMonth=10`

---

### 📚 Supplementary
- **`GET /api/drugs/forms`**  
  _Returns available drug form enum values._

- **`GET /api/drugs/forms/dictionary`**  
  _Returns a dictionary of drug forms and their labels._

- **`GET /api/drugs/statistics`**  
  _Returns statistics (total, expired, active, alerts sent, etc.)._

- **`GET /api/drugs/export/pdf`**  
  _Exports the current drug list to PDF. Supports optional `size` (default `20`, max `100`)._

- **`GET /api/drugs/alert`**  
  _Sends expiry alert emails for drugs expiring in the current month._

---

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

There are two ways to run the application: locally via IntelliJ/Gradle or using Docker.

### ✅ Option 1: Run Locally (IntelliJ or Terminal)

1. Clone the repository:
   ```bash
   git clone https://github.com/NextStepProDev/first_aid_kit.git
   cd first_aid_kit
   ```

2. Make sure you have PostgreSQL running (either locally or via Docker) and that the credentials match the configuration.

   If running the application locally (outside Docker), you must define the required environment variables manually in your system or via IntelliJ:

    - `POSTGRES_USER`
    - `POSTGRES_PASSWORD`
    - `POSTGRES_DB`
    - `MAIL_USERNAME`
    - `MAIL_PASSWORD`

   You can also find example values in the `.env.example` file.

   **Alternative: Use `local` profile (recommended for IntelliJ)**

   The `local` profile (`application-local.yml`) has hardcoded database and mail credentials, so you don't need to set environment variables manually.

   In IntelliJ:
   - Go to **Run → Edit Configurations**
   - Set **Active profiles:** `local`
   - Or add VM option: `-Dspring.profiles.active=local`

3. Run the application:
   ```bash
   # With dev profile (requires env variables)
   ./gradlew bootRun --args='--spring.profiles.active=dev'

   # With local profile (no env variables needed)
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```

4. Access Swagger UI:
   [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)

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

## Project Structure

- `controller/` – REST endpoints
- `service/` – business logic
- `repository/` – data access
- `dto/` – data transfer objects
- `mapper/` – MapStruct mappers
- `configuration/` – Spring & cache configuration
- `bootstrap/` – initial sample data loading
- `integration/` – integration tests (Testcontainers)

## ✅ Tests

This project includes **unit**, **slice (web)**, **integration**, and **E2E** tests using JUnit 5, Spring Test, and Testcontainers.

Run all tests:

```bash
./gradlew test
```

### Test suites

- **Integration** (`src/test/java/.../integration`)
  - `base/AbstractIntegrationTest` — shared Testcontainers/JPA context for integration tests.
  - `web/GlobalExceptionIntegrationTest` — global exception handling & error responses.
  - `e2e/base/DrugCreateE2ETest` — end-to-end flows for creating drugs and triggering email alerts.
  - `e2e/base/SmokeApiTest` — high-level API smoke coverage.

- **Slice / Controller** (`src/test/java/.../slice/controller`)
  - `DrugControllerValidationSliceTest` — controller validation & request mapping (no DB).

- **Unit / Service** (`src/test/java/.../unit/service`)
  - `DrugServiceTest` — pure unit tests of business logic.

### Covered areas
- Request validation (DTO fields & method parameters)
- Error handling (`@ControllerAdvice`) and HTTP status codes
- Search endpoint: filters, pagination, sorting
- PDF export constraints (size limits)
- Email alert logic (including failure path)
- CRUD: create / update / delete / get

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
  "pageable": { "pageNumber": 0, "pageSize": 100 },
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

- Current version: **`0.2.0-SNAPSHOT`**
- Latest stable release: [**`v0.2.0`**](https://github.com/NextStepProDev/first_aid_kit/releases/tag/v0.1.0)

You can browse release history [here](https://github.com/NextStepProDev/first_aid_kit/releases).

### License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

Made with ❤️ during my journey to become a professional Java Developer – [Mateusz Nawratek](https://www.linkedin.com/in/mateusz-nawratek-909752356)