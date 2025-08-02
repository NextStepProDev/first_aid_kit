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


## 📦 API Endpoints

Full documentation available via [Swagger UI](http://localhost:8081/swagger-ui/index.html).

### 🔹 CRUD Operations
- **`GET /api/drugs`**  
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

### 🔎 Filtering & Search
- **`GET /api/drugs/by-name?name={value}`**  
  _Returns drugs matching the given name (case-insensitive)._

- **`GET /api/drugs/by-description?description={text}`**  
  _Searches for drugs containing the given text in their description._

- **`GET /api/drugs/by-form?form={value}`**  
  _Returns drugs matching the given form._

- **`GET /api/drugs/expiration-until?year={yyyy}&month={mm}`**  
  _Returns drugs expiring until the specified year and month._

- **`GET /api/drugs/expired`**  
  _Returns a list of expired drugs._

- **`GET /api/drugs/sorted?sortBy={field}`**  
  _Returns drugs sorted by the specified field (`drugsName`, `expirationDate`, `drugsForm`)._

- **`GET /api/drugs/paged?page=0&size=10&sort=field,asc`**  
  _Returns paginated list of drugs._

---

### 📚 Supplementary
- **`GET /api/drugs/simple`**  
  _Returns a simplified list of drugs (ID, name, form, expiration)._

- **`GET /api/drugs/forms`**  
  _Returns available drug form enum values._

- **`GET /api/drugs/forms/dictionary`**  
  _Returns a dictionary of drug forms and their labels._

- **`GET /api/drugs/statistics`**  
  _Returns statistics (total, expired, active, alerts sent, etc.)._

- **`GET /api/drugs/export/pdf`**  
  _Exports the full drug list to PDF._

- **`GET /api/drugs/alert`**  
  _Sends expiry alert emails for drugs expiring in the current month._

---

### 💾 Performance & Caching

To improve performance, the application uses Spring Cache backed by Caffeine:

- Frequently accessed drug data is cached to reduce database load and improve response time.
- Cached entries expire automatically after 10 minutes.
- Write operations invalidate relevant caches to ensure consistency.
- Configuration is managed via `spring.cache.caffeine.spec` in `application.yml`.
  
## Prerequisites

Before running this application, ensure you have the following installed:

- Java 21
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

3. Run the application (with dev profile):
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=dev'
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
optional but safe cleanup
docker-compose down -v --remove-orphans

build project
./gradlew build

rebuild containers from scratch (skip Docker cache)
docker-compose build --no-cache

start containers
docker-compose up
```
> 📌 Make sure Docker is running before using this method.

### 3. Access the application:
   - Swagger UI: [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
   - pgAdmin: [http://localhost:8080](http://localhost:8080)

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

- Java 21
- Spring Boot
- Spring Web / Validation / Security / Data JPA
- Spring Cache with Caffeine
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

The project includes unit, web, and integration tests using JUnit, Spring Test, and Testcontainers.
Core areas like logic, validation, error handling, and email alerts are covered.  
You can run tests using:

```bash
./gradlew test
```

Covered areas:
- Unit tests (e.g. `DrugsServiceTest`)
- Web layer tests (`DrugsControllerWebMvcTest`)
- Validation logic (e.g. enum mapping, request field validation)
- Integration tests (DrugIntegrationTest with Testcontainers and real PostgreSQL)
- Exception handling
- Email alert logic

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

### Get expiring drugs for specific month

```http
GET /api/drugs/expiring?year=2025&month=10
```

Response example:

```json
[
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
]
```

### License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

Made with ❤️ during my journey to become a professional Java Developer – [Mateusz Nawratek](https://www.linkedin.com/in/mateusz-nawratek-909752356)