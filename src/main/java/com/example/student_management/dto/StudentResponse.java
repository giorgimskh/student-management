package com.example.student_management.dto;

import com.example.student_management.domain.Student;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentResponse(
        UUID id,
        String name,
        String email,
        Instant createdAt,
        List<BookResponse> books,
        List<CourseResponse> courses
) {
    public static StudentResponse from(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getCreatedAt(),
                student.getBooks().stream().map(BookResponse::from).toList(),
                student.getCourses().stream().map(CourseResponse::from).toList()
        );
    }
}
