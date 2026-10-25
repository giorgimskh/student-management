package com.example.student_management.dto;

import com.example.student_management.domain.Book;

import java.util.UUID;

public record BookResponse(
        UUID id,
        String title,
        String isbn
) {
    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getIsbn());
    }
}
