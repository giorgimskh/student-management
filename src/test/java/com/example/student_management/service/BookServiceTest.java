package com.example.student_management.service;

import com.example.student_management.service.impl.BookServiceImpl;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.InvalidCopyCountException;
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
import java.util.List;
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
        bookService = new BookServiceImpl(bookRepository);
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

    @Test
    void copy_keepsTitleAndIsbn_butHasNoIdAndNoBorrower() {
        Book lent = Book.builder().id(UUID.randomUUID()).title("Clean Code").isbn("9780132350884")
                .student(Student.builder().id(UUID.randomUUID()).build()).build();

        Book copy = lent.copy();

        assertThat(copy).isNotSameAs(lent);
        assertThat(copy.getTitle()).isEqualTo("Clean Code");
        assertThat(copy.getIsbn()).isEqualTo("9780132350884");
        assertThat(copy.getId()).isNull();
        assertThat(copy.getStudent()).isNull();
        assertThat(lent.getStudent()).isNotNull(); // the original is untouched
    }

    @Test
    void addCopies_savesThatManyCopiesOfTheBook() {
        UUID id = UUID.randomUUID();
        Book original = Book.builder().id(id).title("Refactoring").isbn("9780134757599").build();
        when(bookRepository.findById(id)).thenReturn(Optional.of(original));
        when(bookRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Book> copies = bookService.addCopies(id, 3);

        assertThat(copies).hasSize(3).doesNotContain(original)
                .allSatisfy(c -> {
                    assertThat(c.getTitle()).isEqualTo("Refactoring");
                    assertThat(c.getIsbn()).isEqualTo("9780134757599");
                });
        assertThat(copies.get(0)).isNotSameAs(copies.get(1)).isNotSameAs(copies.get(2)); // separate objects, not one book saved thrice
    }

    @Test
    void addCopies_rejectsCountsOutsideTheLimit_withoutLookingUpTheBook() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> bookService.addCopies(id, 0)).isInstanceOf(InvalidCopyCountException.class);
        assertThatThrownBy(() -> bookService.addCopies(id, BookService.MAX_COPIES + 1)).isInstanceOf(InvalidCopyCountException.class);
        verifyNoInteractions(bookRepository);
    }
}
