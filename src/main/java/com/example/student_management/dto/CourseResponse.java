package com.example.student_management.dto;

import com.example.student_management.domain.Course;

import java.util.UUID;

public record CourseResponse(
        UUID id,
        String courseName,
        String code
) {
    public static CourseResponse from(Course course) {
        return new CourseResponse(course.getId(), course.getCourseName(), course.getCode());
    }
}
