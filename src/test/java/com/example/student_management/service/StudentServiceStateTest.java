package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Course;
import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentMemento;
import com.example.student_management.event.StudentEventPublisher;
import com.example.student_management.event.StudentEventType;
import com.example.student_management.exceptions.InvalidStudentStateException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.impl.StudentServiceImpl;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** How {@link StudentServiceImpl} applies the student's state to each operation. */
@ExtendWith(MockitoExtension.class)
class StudentServiceStateTest {
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private StudentPhotoService studentPhotoService;
    @Mock
    private StudentEventPublisher eventPublisher;

    @Mock
    private LoanService loanService;

    private StudentService studentService;
    private UUID studentId;
    private Student student;

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(studentRepository, bookRepository, courseRepository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), studentPhotoService, eventPublisher, loanService);
        studentId = UUID.randomUUID();
        student = Student.builder().id(studentId).name("Nino").email("nino@example.com").build();
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
    }

    @Test
    void suspendedStudent_cannotEnroll_andCourseIsNotTouched() {
        student.setStatus(StudentStatus.SUSPENDED);

        assertThatThrownBy(() -> studentService.enrollInCourse(studentId, UUID.randomUUID()))
                .isInstanceOf(InvalidStudentStateException.class)
                .hasMessage("Student " + studentId + " is SUSPENDED and cannot enroll in courses");
        verifyNoInteractions(courseRepository, eventPublisher);
        verify(studentRepository, never()).save(any());
    }

    @Test
    void graduatedStudent_cannotBorrowBooks() {
        student.setStatus(StudentStatus.GRADUATED);

        assertThatThrownBy(() -> studentService.assignBookToStudent(studentId, UUID.randomUUID()))
                .isInstanceOf(InvalidStudentStateException.class)
                .hasMessageContaining("GRADUATED and cannot borrow books");
        verifyNoInteractions(bookRepository, eventPublisher);
    }

    @Test
    void withdrawnStudent_cannotUpdateProfile() {
        student.setStatus(StudentStatus.WITHDRAWN);
        Student details = Student.builder().name("New").email("new@example.com").build();

        assertThatThrownBy(() -> studentService.updateStudent(studentId, details))
                .isInstanceOf(InvalidStudentStateException.class);
        assertThat(student.getName()).isEqualTo("Nino");
        verify(studentRepository, never()).save(any());
    }

    @Test
    void graduatedStudent_cannotHaveProfileRestored() {
        student.setStatus(StudentStatus.GRADUATED);

        StudentMemento old = Student.builder().name("Old").email("old@example.com").build().createMemento();

        assertThatThrownBy(() -> studentService.restoreStudent(studentId, old))
                .isInstanceOf(InvalidStudentStateException.class);
        assertThat(student.getName()).isEqualTo("Nino");
    }

    @Test
    void suspendedStudent_canStillUpdateProfile() {
        student.setStatus(StudentStatus.SUSPENDED);
        when(studentRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(studentRepository.save(student)).thenReturn(student);

        Student updated = studentService.updateStudent(studentId,
                Student.builder().name("New").email("new@example.com").build());

        assertThat(updated.getName()).isEqualTo("New");
    }

    @Test
    void suspendedStudent_canStillGiveBackABook() {
        student.setStatus(StudentStatus.SUSPENDED);
        UUID bookId = UUID.randomUUID();
        Book book = Book.builder().id(bookId).student(student).build();
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        studentService.removeBookFromStudent(studentId, bookId);

        assertThat(book.getStudent()).isNull();
        verify(bookRepository).save(book);
    }

    @Test
    void graduatedStudent_canStillDropACourse() {
        student.setStatus(StudentStatus.GRADUATED);
        UUID courseId = UUID.randomUUID();
        Course course = Course.builder().id(courseId).build();
        student.getCourses().add(course);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        studentService.removeStudentFromCourse(studentId, courseId);

        assertThat(student.getCourses()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE,    SUSPEND,    SUSPENDED, STUDENT_SUSPENDED",
            "SUSPENDED, REACTIVATE, ACTIVE,    STUDENT_REACTIVATED",
            "ACTIVE,    GRADUATE,   GRADUATED, STUDENT_GRADUATED",
            "SUSPENDED, WITHDRAW,   WITHDRAWN, STUDENT_WITHDRAWN"
    })
    void changeStatus_savesNewStatusAndPublishesMatchingEvent(StudentStatus from, StudentTransition transition,
                                                              StudentStatus to, StudentEventType event) {
        student.setStatus(from);
        when(studentRepository.save(student)).thenReturn(student);

        Student result = studentService.changeStatus(studentId, transition);

        assertThat(result.getStatus()).isEqualTo(to);
        verify(studentRepository).save(student);
        verify(eventPublisher).publish(event, studentId);
    }

    @Test
    void changeStatus_notAllowed_savesNothingAndPublishesNothing() {
        student.setStatus(StudentStatus.WITHDRAWN);

        assertThatThrownBy(() -> studentService.changeStatus(studentId, StudentTransition.REACTIVATE))
                .isInstanceOf(InvalidStudentStateException.class);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.WITHDRAWN);
        verify(studentRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void restoreStatus_bypassesTransitionRules() {
        student.setStatus(StudentStatus.GRADUATED); // GRADUATED -> ACTIVE is not a normal transition
        when(studentRepository.save(student)).thenReturn(student);

        Student result = studentService.restoreStatus(studentId, StudentStatus.ACTIVE);

        assertThat(result.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        verify(eventPublisher).publish(StudentEventType.STUDENT_STATUS_RESTORED, studentId);
    }
}
