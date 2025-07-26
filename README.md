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

## Setup & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/NextStepProDev/first_aid_kit.git
   cd first_aid_kit
   ```

2. Configure the database connection:
   Edit `src/main/resources/application.yml` or use the `.env` file.

3. Run the application:
   ```bash
   ./gradlew bootRun
   ```

4. Access Swagger UI:
   [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Performance Optimizations

The application uses Spring Cache with a Caffeine backend to improve performance:
- Caches method results such as drug listings and queries.
- Cached entries expire automatically after 10 minutes.
- Write operations invalidate relevant caches to ensure consistency.
- Configuration is managed via `spring.cache.caffeine.spec` in `application.yml`.


## 📚 API Documentation

Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Technologies Used

- Java 21
- Spring Boot
- Spring Web / Validation / Security / Data JPA
- Spring Cache with Caffeine
- MapStruct
- PostgreSQL
- Flyway
- OpenAPI / Swagger

## Project Structure

- `controller/` – REST endpoints
- `service/` – business logic
- `repository/` – data access
- `dto/` – data transfer objects
- `mapper/` – MapStruct mappers
- `configuration/` – Spring & cache configuration
- `bootstrap/` – initial sample data loading

## ✅ Tests

This project includes unit and integration tests using JUnit and Spring Test.  
You can run tests using:

```bash
./gradlew test
```

Covered areas:
- Unit tests (e.g. `DrugsServiceTest`)
- Web layer tests (`DrugsControllerWebMvcTest`)
- Validation logic (e.g. enum mapping, request field validation)
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
  "name": "Paracetamol",
  "description": "Painkiller",
  "expirationDate": "2025-12-31T00:00:00Z",
  "form": "PILL"
}
```

## 🐳 Docker Setup (PostgreSQL & pgAdmin)

This project includes Docker configuration for running PostgreSQL and pgAdmin. Follow these steps to set it up locally.

### 1. Environment Configuration

Create a `.env` file in the project root (next to `docker-compose.yml`). Use the `.env.example` file as a template:

```dotenv
# PostgreSQL
POSTGRES_DB=first_aid_kit
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

# pgAdmin
PGADMIN_DEFAULT_EMAIL=admin@example.com
PGADMIN_DEFAULT_PASSWORD=admin1234
```

> 🔒 **Do not commit the `.env` file to version control** — it's excluded via `.gitignore`.

### 2. Run the containers

From the root directory of the project, run:

```bash
docker-compose up -d
```

This will start two containers:
- `postgres-drugs` (PostgreSQL 17.4)
- `pgadmin-drugs` (pgAdmin interface)

### 3. Access pgAdmin

- Open your browser and go to: [http://localhost:8080](http://localhost:8080)
- Log in using:
  - **Email:** `admin@example.com`
  - **Password:** `admin1234`

> 📌 You can customize these credentials in the `.env` file.

### 4. Persistent database data

PostgreSQL data is stored in a Docker volume:

```yaml
volumes:
  postgres_data:
    name: 00c986efb2500a05bc21d82f814224f8c063dbd0b80045157352ec1501ddd314
```

This means your database data will persist even if you remove the container.  
To view or manage volumes:

```bash
docker volume ls
docker volume inspect 00c986...
```

License

## License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
---
❤️ Made with love during my journey to become a Junior Java Developer – Mateusz Nawratek