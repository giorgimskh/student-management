package com.example.student_management.mapper;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.dto.BookRequest;
import com.example.student_management.dto.BookResponse;
import com.example.student_management.dto.CourseRequest;
import com.example.student_management.dto.CourseResponse;
import com.example.student_management.dto.StudentRequest;
import com.example.student_management.dto.StudentResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EntityMapperTest {
    private final StudentMapper studentMapper = new StudentMapper();
    private final BookMapper bookMapper = new BookMapper();
    private final CourseMapper courseMapper = new CourseMapper();

    @Test
    void studentMapper_toEntity_copiesRequestFields() {
        Student student = studentMapper.toEntity(new StudentRequest("Nino", "nino@example.com"));

        assertThat(student.getId()).isNull();
        assertThat(student.getName()).isEqualTo("Nino");
        assertThat(student.getEmail()).isEqualTo("nino@example.com");
        assertThat(student.getBooks()).isEmpty();
        assertThat(student.getCourses()).isEmpty();
    }

    @Test
    void studentMapper_toResponse_includesBooksAndCourses() {
        Book book = Book.builder().id(UUID.randomUUID()).title("Clean Code").isbn("0-306-40615-2").build();
        Course course = Course.builder().id(UUID.randomUUID()).courseName("Databases").code("CS201").build();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Student student = Student.builder()
                .id(UUID.randomUUID()).name("Nino").email("nino@example.com").createdAt(createdAt)
                .books(List.of(book)).courses(Set.of(course))
                .build();

        StudentResponse response = studentMapper.toResponse(student);

        assertThat(response.id()).isEqualTo(student.getId());
        assertThat(response.name()).isEqualTo("Nino");
        assertThat(response.email()).isEqualTo("nino@example.com");
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.books()).containsExactly(new BookResponse(book.getId(), "Clean Code", "0-306-40615-2"));
        assertThat(response.courses()).containsExactly(new CourseResponse(course.getId(), "Databases", "CS201"));
    }

    @Test
    void bookMapper_roundTripsFields() {
        Book book = bookMapper.toEntity(new BookRequest("Refactoring", "978-0-13-475759-9"));
        assertThat(book.getTitle()).isEqualTo("Refactoring");
        assertThat(book.getIsbn()).isEqualTo("978-0-13-475759-9");
        assertThat(book.getStudent()).isNull();

        book.setId(UUID.randomUUID());
        assertThat(bookMapper.toResponse(book))
                .isEqualTo(new BookResponse(book.getId(), "Refactoring", "978-0-13-475759-9"));
    }

    @Test
    void courseMapper_roundTripsFields() {
        Course course = courseMapper.toEntity(new CourseRequest("Algorithms", "CS301"));
        assertThat(course.getCourseName()).isEqualTo("Algorithms");
        assertThat(course.getCode()).isEqualTo("CS301");
        assertThat(course.getStudents()).isEmpty();

        course.setId(UUID.randomUUID());
        assertThat(courseMapper.toResponse(course))
                .isEqualTo(new CourseResponse(course.getId(), "Algorithms", "CS301"));
    }

    @Test
    void toResponses_mapsEveryElementOfAnyCollection() {
        Course a = Course.builder().id(UUID.randomUUID()).courseName("A").code("A1").build();
        Course b = Course.builder().id(UUID.randomUUID()).courseName("B").code("B1").build();

        assertThat(courseMapper.toResponses(Set.of(a, b)))
                .extracting(CourseResponse::courseName)
                .containsExactlyInAnyOrder("A", "B");
        assertThat(courseMapper.toResponses(List.of(a)))
                .containsExactly(new CourseResponse(a.getId(), "A", "A1"));
    }

    @Test
    void toResponses_ofEmptyCollection_isEmpty() {
        assertThat(studentMapper.toResponses(Set.of())).isEmpty();
    }
}
