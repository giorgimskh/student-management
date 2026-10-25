package com.example.student_management.controller;

import com.example.student_management.dto.BookRequest;
import com.example.student_management.dto.BookResponse;
import com.example.student_management.dto.PagedResponse;
import com.example.student_management.dto.StudentResponse;
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

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<BookResponse>> getAllBooks(@PageableDefault(size = 10, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(PagedResponse.from(bookService.getAllBooks(pageable).map(BookResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(@PathVariable UUID id) {
        return ResponseEntity.ok(BookResponse.from(bookService.getBookById(id)));
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.ok(BookResponse.from(bookService.createBook(request.toEntity())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable UUID id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bookId}/owner")
    public ResponseEntity<StudentResponse> getBookOwner(@PathVariable UUID bookId) {
        return ResponseEntity.ok(StudentResponse.from(bookService.getBookOwner(bookId)));
    }
}
