package com.example.student_management.dto;

import com.example.student_management.domain.Book;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BookRequest(
        @NotBlank(message = "Book Title is required")
        String title,

        @NotBlank(message = "ISBN cannot be blank")
        @Pattern(regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|97[89][0-9]{10}$|(?=(?:[0-9]+[-]-?){3}[0-9X]{1}$|97[89](?:-[0-9]+){3}[0-9]{1}$))[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
                message = "ISBN must be 10 or 13 digits")
        String isbn
) {
    public Book toEntity() {
        return Book.builder()
                .title(title)
                .isbn(isbn)
                .build();
    }
}
