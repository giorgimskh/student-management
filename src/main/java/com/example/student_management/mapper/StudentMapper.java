package com.example.student_management.mapper;

import com.example.student_management.domain.Student;
import com.example.student_management.dto.StudentRequest;
import com.example.student_management.dto.StudentResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper implements EntityMapper<Student, StudentRequest, StudentResponse> {
    @Override
    public Student toEntity(StudentRequest request) {
        return request.toEntity();
    }

    @Override
    public StudentResponse toResponse(Student student) {
        return StudentResponse.from(student);
    }
}
