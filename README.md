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

This is a Java-based application designed to manage a database of drugs and export the current drug list to PDF. The application includes functionality for managing drug records, including creating, updating, deleting, and querying drugs. It also supports email alerts for drugs that are nearing their expiration dates.

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
  
- **API Endpoints**:
  - List all drugs (`GET /api/drugs`).
  - Get drug details by name (`GET /api/drugs/by-name?name={name}`).
  - Update a drug (`PUT /api/drugs`).
  - Delete a drug (`DELETE /api/drugs/{id}`).
  - Get drug statistics (`GET /api/drugs/statistics`).
  - Add a new drug (`POST /api/drugs`)
  - Get simple list of drugs (`GET /api/drugs/simple`)
  - Get expired drugs (`GET /api/drugs/expired`)
  - Get drugs expiring in a specific month (`GET /api/drugs/expiring?year={year}&month={month}`)
  - Get sorted drugs (`GET /api/drugs/sorted?sortBy={field}`)
  - Export drugs list to PDF (`GET /api/drugs/export/pdf`) → returns a downloadable PDF file
  - Send test email (`GET /api/email/test`)
  - Send expiry alert emails (`GET /api/email/alert`)

- **Caching for performance**:
  - Frequently accessed drug data is cached to reduce database load and improve response time.
  
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

## Performance Optimizations

The application uses Spring Cache with a Caffeine backend to improve performance:
- Caches method results such as drug listings and queries.
- Cached entries expire automatically after 10 minutes.
- Write operations invalidate relevant caches to ensure consistency.
- Configuration is managed via `spring.cache.caffeine.spec` in `application.yml`.


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

## ✅ Tests

This project includes unit and integration tests (Testcontainers and real PostgreSQL) using JUnit and Spring Test.  
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
    "drugsId": 1,
    "drugsName": "Paracetamol",
    "drugsForm": "PILL",
    "expirationDate": "2025-10-31T00:00:00Z",
    "drugsDescription": "Painkiller"
  },
  {
    "drugsId": 2,
    "drugsName": "Ibuprofen",
    "drugsForm": "GEL",
    "expirationDate": "2025-10-15T00:00:00Z",
    "drugsDescription": "Used to treat pain and fever"
  }
]
```

### License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.

Made with ❤️ during my journey to become a Java Developer – [Mateusz Nawratek](https://www.linkedin.com/in/mateusz-nawratek-909752356)