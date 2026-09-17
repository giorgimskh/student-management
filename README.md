# Student Management

A Spring Boot REST API for managing students, their book loans, course enrollments, and ID photos.

## Features

- **Student records** — create, read, update, delete, and search (by name, paginated). Email is required and validated on creation.
- **Book lending** — track books by title/ISBN and assign them to at most one student at a time; a book must be returned before it can be reassigned.
- **Course enrollment** — enroll/un-enroll students in courses (many-to-many); a student cannot be enrolled in the same course twice.
- **Student photos** — upload, retrieve, and delete a single ID photo per student (max 2 MB per file).
- **Referential integrity** — deleting a student cascades to their books and photo; deleting a course detaches it from every enrolled student.
- **Paginated, validated API** — list endpoints support pagination/sorting; request bodies are validated with structured JSON error responses.

## Tech Stack

- Java 25, Spring Boot 4.1 (Web MVC, Data JPA, Validation)
- PostgreSQL
- springdoc-openapi (Swagger UI)
- JUnit 5 / Mockito, H2 (test scope)
- Lombok

## Getting Started

### Prerequisites

- Java 25
- Maven
- PostgreSQL running locally

### Configuration

Set your database connection in `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/<your-db>
spring.datasource.username=<your-username>
spring.datasource.password=<your-password>
```

The schema is created/updated automatically on startup (`spring.jpa.hibernate.ddl-auto=update`).

### Run

```bash
./mvnw spring-boot:run
```

The API is served at `http://localhost:8080`. Swagger UI is available at `http://localhost:8080/swagger-ui.html`.

### Authentication

All `/api/**` requests must include an `X-Api-Key` header. See `SimpleApiKeyFilter` for the configured key.

### Tests

```bash
./mvnw test
```

## API Overview

| Resource | Endpoints |
|---|---|
| Students | `GET/POST /api/students`, `GET/PUT/DELETE /api/students/{id}`, `GET /api/students/search?name=` |
| Books | `GET/POST /api/books`, `GET/DELETE /api/books/{id}`, `GET /api/books/{id}/owner` |
| Courses | `GET/POST /api/courses`, `GET/DELETE /api/courses/{id}`, `GET /api/courses/{id}/students` |
| Book assignment | `POST/DELETE /api/students/{studentId}/books/{bookId}`, `GET /api/students/{studentId}/books` |
| Enrollment | `POST/DELETE /api/students/{studentId}/courses/{courseId}`, `GET /api/students/{studentId}/courses` |
| Student photo | `POST/GET/DELETE /api/students/{studentId}/photo` |

## Known Limitations

- Access control is a single shared API key — there are no user identities, roles, or per-resource authorization.
- Email uniqueness is enforced on creation only, not on update.
- Photo uploads are not validated for content type or emptiness beyond the configured size limit.
