package com.example.student_management.export;

import com.example.student_management.domain.Student;
import com.example.student_management.repository.StudentRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class StudentExporter extends TableExporter<Student> {
    private final StudentRepository studentRepository;

    public StudentExporter(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    public String type() {
        return "students";
    }

    @Override
    protected List<String> headers() {
        return List.of("id", "name", "email", "status", "created_at");
    }

    @Override
    protected List<Student> records() {
        return studentRepository.findAll(Sort.by("name"));
    }

    @Override
    protected List<?> toRow(Student s) {
        return Arrays.asList(s.getId(), s.getName(), s.getEmail(), s.getStatus(), s.getCreatedAt());
    }
}
