package com.example.student_management.mapper;

import com.example.student_management.domain.Book;
import com.example.student_management.dto.BookRequest;
import com.example.student_management.dto.BookResponse;
import org.springframework.stereotype.Component;

@Component
public class BookMapper implements EntityMapper<Book, BookRequest, BookResponse> {
    @Override
    public Book toEntity(BookRequest request) {
        return request.toEntity();
    }

    @Override
    public BookResponse toResponse(Book book) {
        return BookResponse.from(book);
    }
}
