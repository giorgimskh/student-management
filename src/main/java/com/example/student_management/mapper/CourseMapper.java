package com.example.student_management.mapper;

import com.example.student_management.domain.Course;
import com.example.student_management.dto.CourseRequest;
import com.example.student_management.dto.CourseResponse;
import org.springframework.stereotype.Component;

@Component
public class CourseMapper implements EntityMapper<Course, CourseRequest, CourseResponse> {
    @Override
    public Course toEntity(CourseRequest request) {
        return request.toEntity();
    }

    @Override
    public CourseResponse toResponse(Course course) {
        return CourseResponse.from(course);
    }
}
