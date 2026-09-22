# UML Diagrams — student-management

This document describes the design of the `student-management` Spring Boot application as two UML class diagrams, rendered with [Mermaid](https://mermaid.js.org/) so they display natively on GitHub and in most IDEs.

The codebase (base package `com.example.student_management`) is organized into:

- `domain/` — the 4 JPA entities (`Student`, `Book`, `Course`, `StudentPhoto`)
- `repository/` — Spring Data JPA repository interfaces, one per entity
- `service/` — business logic, one service per entity/aggregate
- `controller/` — REST controllers exposing `/api/**` endpoints, one per entity
- `dto/` — response-shaping records (`PagedResponse<T>`, `PhotoResponseDto`) not shown below, as they don't mirror the domain model
- `exceptions/`, `filter/`, `interceptor/`, `config/` — cross-cutting infrastructure (error handling, logging, API-key auth, MVC config), omitted from these diagrams since they add no class-association structure

There is no inheritance or enum usage anywhere in the domain model.

## 1. Domain model

Entities, fields, and JPA-derived relationships/cardinalities.

```mermaid
classDiagram
    class Student {
        -UUID id
        -String name
        -String email
        -Instant createdAt
        -List~Book~ books
        -Set~Course~ courses
        -StudentPhoto studentPhoto
    }

    class Book {
        -UUID id
        -String title
        -String isbn
        -Student student
    }

    class Course {
        -UUID id
        -String courseName
        -String code
        -Set~Student~ students
    }

    class StudentPhoto {
        -UUID id
        -Student student
        -byte[] data
        -String contentType
        -String originalFilename
        -long sizeBytes
        -Instant uploadedAt
    }

    Student "1" *-- "0..*" Book : books\n(cascade ALL, orphanRemoval)
    Student "1" *-- "0..1" StudentPhoto : studentPhoto\n(cascade ALL, orphanRemoval)
    Student "0..*" -- "0..*" Course : courses\n(join table student_courses)
```

**Notes**
- `Student` ↔ `Book`: bidirectional one-to-many, owning side `Book.student` (FK `books.student_id`). Deleting a `Student` deletes their `Book`s (composition).
- `Student` ↔ `Course`: bidirectional many-to-many, owning side `Student.courses`, via join table `student_courses(student_id, course_id)`.
- `Student` ↔ `StudentPhoto`: bidirectional one-to-one, owning side `StudentPhoto.student` (unique FK `student_photos.student_id`). Deleting a `Student` deletes their photo (composition).

## 2. Layered architecture

Controllers depend on services, services depend on repositories (and, for `StudentService`, on `StudentPhotoService`), and repositories manage entities. Method lists are abbreviated to their signatures.

```mermaid
classDiagram
    %% ---- Controllers ----
    class StudentController {
        +getAllStudents(Pageable) ResponseEntity~PagedResponse~Student~~
        +searchStudents(String, Pageable) ResponseEntity~PagedResponse~Student~~
        +getStudentById(UUID) ResponseEntity~Student~
        +createStudent(Student) ResponseEntity~Student~
        +updateStudent(UUID, Student) ResponseEntity~Student~
        +deleteStudent(UUID) ResponseEntity~Void~
        +assignBookToStudent(UUID, UUID) ResponseEntity~Student~
        +removeBookFromStudent(UUID, UUID) ResponseEntity~Void~
        +getBooksByStudent(UUID, Pageable) ResponseEntity~PagedResponse~Book~~
        +enrollStudentInCourse(UUID, UUID) ResponseEntity~Student~
        +removeStudentFromCourse(UUID, UUID) ResponseEntity~Void~
        +getCoursesByStudent(UUID) ResponseEntity~Set~Course~~
    }

    class BookController {
        +getAllBooks(Pageable) ResponseEntity~PagedResponse~Book~~
        +getBookById(UUID) ResponseEntity~Book~
        +createBook(Book) ResponseEntity~Book~
        +deleteBook(UUID) ResponseEntity~Void~
        +getBookOwner(UUID) ResponseEntity~Student~
    }

    class CourseController {
        +getAllCourses(Pageable) ResponseEntity~PagedResponse~Course~~
        +getCourseById(UUID) ResponseEntity~Course~
        +createCourse(Course) ResponseEntity~Course~
        +deleteCourse(UUID) ResponseEntity~Void~
        +getStudentsInCourse(UUID) ResponseEntity~Set~Student~~
    }

    class StudentPhotoController {
        +uploadPhoto(UUID, MultipartFile) ResponseEntity~StudentPhoto~
        +downloadPhoto(UUID) ResponseEntity~Resource~
        +deletePhoto(UUID) ResponseEntity~Void~
    }

    %% ---- Services ----
    class StudentService {
        +getAllStudents(Pageable) Page~Student~
        +createStudent(Student) Student
        +getStudentById(UUID) Student
        +updateStudent(UUID, Student) Student
        +deleteStudent(UUID) void
        +assignBookToStudent(UUID, UUID) Student
        +removeBookFromStudent(UUID, UUID) void
        +getBooksByStudent(UUID, Pageable) Page~Book~
        +enrollInCourse(UUID, UUID) Student
        +removeStudentFromCourse(UUID, UUID) void
        +getCoursesByStudent(UUID) Set~Course~
        +searchStudents(String, Pageable) Page~Student~
    }

    class BookService {
        +getAllBooks(Pageable) Page~Book~
        +getBookById(UUID) Book
        +createBook(Book) Book
        +deleteBook(UUID) void
        +getBookOwner(UUID) Student
    }

    class CourseService {
        +getAllCourses(Pageable) Page~Course~
        +getCourseById(UUID) Course
        +createCourse(Course) Course
        +deleteCourse(UUID) void
        +getStudentsInCourse(UUID) Set~Student~
    }

    class StudentPhotoService {
        +storePhoto(UUID, MultipartFile) StudentPhoto
        +getPhoto(UUID) PhotoResponseDto
        +deletePhoto(UUID) void
        +deletePhotoIfExists(UUID) void
    }

    %% ---- Repositories ----
    class StudentRepository {
        <<interface>>
        +findByEmail(String) Optional~Student~
        +findByNameContainingIgnoreCase(String, Pageable) Page~Student~
    }

    class BookRepository {
        <<interface>>
        +findByStudentId(UUID, Pageable) Page~Book~
    }

    class CourseRepository {
        <<interface>>
    }

    class StudentPhotoRepository {
        <<interface>>
        +findByStudentId(UUID) Optional~StudentPhoto~
        +deleteByStudentId(UUID) void
    }

    %% ---- Domain entities (see diagram 1 for fields/relationships) ----
    class Student
    class Book
    class Course
    class StudentPhoto

    %% ---- Layer dependencies ----
    StudentController ..> StudentService : uses
    BookController ..> BookService : uses
    CourseController ..> CourseService : uses
    StudentPhotoController ..> StudentPhotoService : uses

    StudentService ..> StudentRepository : uses
    StudentService ..> BookRepository : uses
    StudentService ..> CourseRepository : uses
    StudentService ..> StudentPhotoService : uses
    BookService ..> BookRepository : uses
    CourseService ..> CourseRepository : uses
    StudentPhotoService ..> StudentPhotoRepository : uses
    StudentPhotoService ..> StudentRepository : uses

    StudentRepository ..> Student : manages
    BookRepository ..> Book : manages
    CourseRepository ..> Course : manages
    StudentPhotoRepository ..> StudentPhoto : manages
```
