package com.example.student_management.service.impl;

import com.example.student_management.service.BookService;
import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {
    private final BookRepository bookRepository;

    public BookServiceImpl(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Override
    public Book getBookById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    @Override
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    @Override
    public void deleteBook(UUID id) {
        Book book = getBookById(id);

        if (book.getStudent() != null) {
            book.getStudent().getBooks().remove(book);
        }

        bookRepository.delete(book);
    }

    @Override
    public Student getBookOwner(UUID bookId) {
        Book book = getBookById(bookId);

        if (book.getStudent() == null) {
            throw new ResourceNotFoundException("Book with id: " + bookId + " has no owner");
        }
        return book.getStudent();
    }
}
