package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.BookRepository;
import com.example.student_management.repository.CourseRepository;
import com.example.student_management.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookServiceTest {
    @Mock
    private BookRepository bookRepository;

    private BookService bookService;

    @BeforeEach
    void setUp() {
        bookService = new BookService(bookRepository);
    }

    @Test
    public void deleteBook_removesBookFromStudent_whenBookHasOwner(){
       UUID bookId=UUID.randomUUID();
       UUID studentId = UUID.randomUUID();

       Student student = Student.builder()
                .id(studentId)
                .books(new ArrayList<>())
                .build();

       Book book = Book.builder()
               .id(bookId)
               .student(student)
               .build();

       student.getBooks().add(book);

       when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

       bookService.deleteBook(bookId);
       assertThat(student.getBooks()).doesNotContain(book);
       verify(bookRepository).delete(book);
    }

    @Test
    public void getBookOwner_throwsResourceNotFoundException_whenBookHasNoOwner(){
        UUID bookId=UUID.randomUUID();

        Book book = Book.builder()
                .id(bookId)
                .student(null)
                .build();

        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        assertThatThrownBy(()->bookService.getBookOwner(bookId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(bookId.toString());
    }


}
