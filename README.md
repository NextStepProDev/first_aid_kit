# First Aid Kit Application

This is a Java-based application designed to manage a database of drugs. The application includes functionality for managing drug records, including creating, updating, deleting, and querying drugs. It also supports email alerts for drugs that are nearing their expiration dates.

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

- **Caching for performance**:
  - Frequently accessed drug data is cached to reduce database load and improve response time.
  
## Prerequisites

Before running this application, ensure you have the following installed:

- Java 21
- Gradle (build system used in this project)
- PostgreSQL (or any compatible relational database)
- Docker (optional, for containerized setup)

## Setup

## Performance Optimizations

The application uses Spring Cache with a Caffeine backend to improve performance:
- Caches method results such as drug listings and queries.
- Cached entries expire automatically after 10 minutes.
- Write operations (`POST`, `PUT`, `DELETE`) clear relevant caches to maintain consistency.
- Configuration is managed via `spring.cache.caffeine.spec` in `application.yml`.


1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/first-aid-kit.git
   cd first-aid-kit

2. Configure the application:
	•	Set up the database connection in application.properties or application.yml.
	•	Optionally, configure email settings for alert notifications.


API Documentation

The application exposes a REST API to interact with the drug records. You can view the API documentation using Swagger at:

http://localhost:8080/swagger-ui.html

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

License

This project is licensed under the MIT License - see the LICENSE file for details.
