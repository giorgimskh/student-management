package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.BookAlreadyAssignedException;
import com.example.student_management.exceptions.DuplicateEmailException;
import com.example.student_management.exceptions.InvalidEnrollmentException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class StudentServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private CourseRepository courseRepository;

    private Student student;
    private UUID studentId;

    @Captor
    private ArgumentCaptor<Student> studentCaptor;


    private StudentService studentService;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        // Built manually instead of via @InjectMocks because Clock isn't a
        // Mockito mock here (it's a fixed real instance) — @InjectMocks only
        // wires fields annotated with @Mock/@Spy. This pattern (manual wiring
        // for a mix of mocks + real config values) is very common in real code.
        studentService = new StudentService(studentRepository, bookRepository, courseRepository, fixedClock);

        studentId = UUID.randomUUID();
        student = Student.builder()
                .id(studentId)
                .name("Nino Kapanadze")
                .email("nino@example.com")
                .build();
    }

    @Test
    public void createStudent_whenEmailIsUnique(){
        when(studentRepository.findByEmail(student.getEmail())).thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class))).thenAnswer(inv->inv.getArgument(0));

        Student result=studentService.createStudent(student);

        verify(studentRepository).save(studentCaptor.capture());
        Student savedStudent=studentCaptor.getValue();

        assertThat(savedStudent).isEqualTo(result);
        assertThat(savedStudent.getEmail()).isEqualTo("nino@example.com");
        assertThat(savedStudent.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(result.getCreatedAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    public void createStudent_throwsDuplicateEmailException_whenEmailAlreadyExists(){
        when(studentRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));

        //checks that exception is thrown when creating student with duplicate email
        assertThatThrownBy(() -> studentService.createStudent(student)).
                isInstanceOf(DuplicateEmailException.class).hasMessageContaining(student.getEmail());


        //verifies that save method is never called
        verify(studentRepository,never()).save(any());
    }

    @Test
    public void getStudentById_throwsResourceNotFoundException_whenStudentIsNotFound(){
        when(studentRepository.findById(studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(()->studentService.getStudentById(studentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(studentId.toString());
    }

    @Test
    public void assignBookToStudent_throwsBookAlreadyAssignedException_whenBookHasOwner(){
        UUID bookId=UUID.randomUUID();
        Book book  = Book.builder().id(bookId).
                student(Student.builder().build()).
                build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));


        assertThatThrownBy(()->studentService.assignBookToStudent(studentId,bookId))
                .isInstanceOf(BookAlreadyAssignedException.class);

        verify(bookRepository,never()).save(any());
    }

    @Test
    public void assignBookToStudent_assignsBook_whenBookIsUnowned(){
        Student spyStudent=spy(Student.builder().id(studentId).name("Spy Student").build());

        UUID bookId=UUID.randomUUID();
        Book book  = Book.builder().id(bookId).
                student(null).
                build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(spyStudent));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(bookRepository.save(book)).thenReturn(book);


        Student result=studentService.assignBookToStudent(studentId,bookId);

        assertThat(result.getBooks()).contains(book);
        assertThat(book.getStudent()).isEqualTo(spyStudent);
        verify(spyStudent,atLeastOnce()).getBooks();
        verify(bookRepository).save(book);

    }

    @Test
    public void enrollInCourse_throwsInvalidEnrollmentException_whenAlreadyEnrolled(){
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder()
                .id(courseId)
                .courseName("Databases")
                .students(new HashSet<>(Set.of(student))) // already contains this student
                .build();

        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(()->studentService.enrollInCourse(studentId,courseId))
                .isInstanceOf(InvalidEnrollmentException.class)
                .hasMessageContaining("Databases");

        verify(studentRepository,never()).save(any());

    }

}
