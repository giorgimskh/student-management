package com.example.student_management.service;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/** Business operations on books. */
public interface BookService {
    List<Book> getAllBooks();

    Page<Book> getAllBooks(Pageable pageable);

    Book getBookById(UUID id);

    Book createBook(Book book);

    void deleteBook(UUID id);

    /** Most copies {@link #addCopies} makes in one call. */
    int MAX_COPIES = 20;

    /**
     * Adds {@code count} more copies of a book, made from it with
     * {@link Book#copy()}. The original is unchanged.
     *
     * @throws com.example.student_management.exceptions.InvalidCopyCountException if count is not between 1 and {@link #MAX_COPIES}
     */
    List<Book> addCopies(UUID bookId, int count);

    /** The student who currently holds the book; 404 if it is unassigned. */
    Student getBookOwner(UUID bookId);
}
