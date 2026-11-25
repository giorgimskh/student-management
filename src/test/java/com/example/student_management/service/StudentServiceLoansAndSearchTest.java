package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.event.StudentEventPublisher;
import com.example.student_management.exceptions.BookAlreadyAssignedException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.impl.StudentServiceImpl;
import com.example.student_management.state.StudentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** How {@link StudentServiceImpl} keeps loans in step with lending, and its combined name/status search. */
@ExtendWith(MockitoExtension.class)
class StudentServiceLoansAndSearchTest {
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
    private final Pageable pageable = PageRequest.of(0, 10);
    private final Page<Student> page = new PageImpl<>(List.of());

    @BeforeEach
    void setUp() {
        studentService = new StudentServiceImpl(studentRepository, bookRepository, courseRepository,
                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC), studentPhotoService, eventPublisher, loanService);
    }

    @Test
    void lendingABook_opensALoan() {
        Student student = Student.builder().id(UUID.randomUUID()).build();
        Book book = Book.builder().id(UUID.randomUUID()).build();
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        studentService.assignBookToStudent(student.getId(), book.getId());

        verify(loanService).openLoan(book, student);
    }

    @Test
    void lendingABookSomeoneElseHas_opensNoLoan() {
        Student student = Student.builder().id(UUID.randomUUID()).build();
        Book book = Book.builder().id(UUID.randomUUID()).student(Student.builder().id(UUID.randomUUID()).build()).build();
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> studentService.assignBookToStudent(student.getId(), book.getId()))
                .isInstanceOf(BookAlreadyAssignedException.class);
        verifyNoInteractions(loanService);
    }

    @Test
    void givingBackABook_closesItsLoan() {
        Student student = Student.builder().id(UUID.randomUUID()).build();
        Book book = Book.builder().id(UUID.randomUUID()).student(student).build();
        when(studentRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(bookRepository.findById(book.getId())).thenReturn(Optional.of(book));

        studentService.removeBookFromStudent(student.getId(), book.getId());

        verify(loanService).closeLoan(book.getId());
    }

    @Test
    void findStudents_nameAndStatus() {
        when(studentRepository.findByNameContainingIgnoreCaseAndStatus("ana", StudentStatus.SUSPENDED, pageable)).thenReturn(page);

        assertThat(studentService.findStudents(" ana ", StudentStatus.SUSPENDED, pageable)).isSameAs(page);
    }

    @Test
    void findStudents_statusOnly_whenNameIsBlank() {
        when(studentRepository.findByStatus(StudentStatus.GRADUATED, pageable)).thenReturn(page);

        assertThat(studentService.findStudents("  ", StudentStatus.GRADUATED, pageable)).isSameAs(page);
        verify(studentRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    void findStudents_nameOnly() {
        when(studentRepository.findByNameContainingIgnoreCase("ana", pageable)).thenReturn(page);

        assertThat(studentService.findStudents("ana", null, pageable)).isSameAs(page);
    }

    @Test
    void findStudents_noFilters_returnsEveryone() {
        when(studentRepository.findAll(pageable)).thenReturn(page);

        assertThat(studentService.findStudents(null, null, pageable)).isSameAs(page);
    }
}
