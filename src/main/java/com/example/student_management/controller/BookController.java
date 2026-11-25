package com.example.student_management.controller;

import com.example.student_management.domain.Book;
import com.example.student_management.domain.Student;
import com.example.student_management.dto.BookRequest;
import com.example.student_management.dto.BookResponse;
import com.example.student_management.dto.PagedResponse;
import com.example.student_management.dto.StudentRequest;
import com.example.student_management.dto.StudentResponse;
import com.example.student_management.mapper.EntityMapper;
import com.example.student_management.service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookController {
    private final BookService bookService;
    private final EntityMapper<Book, BookRequest, BookResponse> bookMapper;
    private final EntityMapper<Student, StudentRequest, StudentResponse> studentMapper;

    public BookController(BookService bookService,
                          EntityMapper<Book, BookRequest, BookResponse> bookMapper,
                          EntityMapper<Student, StudentRequest, StudentResponse> studentMapper) {
        this.bookService = bookService;
        this.bookMapper = bookMapper;
        this.studentMapper = studentMapper;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BookResponse>> getAllBooks(@PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(bookService.getAllBooks(pageable).map(bookMapper::toResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(bookMapper.toResponse(bookService.getBookById(id)));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(bookMapper.toResponse(bookService.createBook(bookMapper.toEntity(request))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookId}/owner")
    public ResponseEntity<StudentResponse> getBookOwner(@PathVariable UUID bookId) {
        return ResponseEntity.ok(studentMapper.toResponse(bookService.getBookOwner(bookId)));
    }
}
