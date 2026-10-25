package com.example.student_management.dto;

import com.example.student_management.domain.Student;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record StudentRequest(
        @NotBlank(message = "Student name is required")
        String name,

        @Email(message = "Email is required")
        @NotBlank(message = "Email cant be blank")
        String email
) {
    public Student toEntity() {
        return Student.builder()
                .name(name)
                .email(email)
                .build();
    }
}
