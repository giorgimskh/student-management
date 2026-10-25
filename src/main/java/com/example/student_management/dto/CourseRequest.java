package com.example.student_management.dto;

import com.example.student_management.domain.Course;
import jakarta.validation.constraints.NotBlank;

public record CourseRequest(
        @NotBlank(message = "Course name is required")
        String courseName,

        @NotBlank(message = "Course code is required")
        String code
) {
    public Course toEntity() {
        return Course.builder()
                .courseName(courseName)
                .code(code)
                .build();
    }
}
