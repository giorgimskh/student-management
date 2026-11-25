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

    /** The student who currently holds the book; 404 if it is unassigned. */
    Student getBookOwner(UUID bookId);
}
