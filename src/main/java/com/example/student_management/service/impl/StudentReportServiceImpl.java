package com.example.student_management.service.impl;

import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.service.StudentReportService;
import com.example.student_management.visitor.StudentGraphVisitor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The transaction keeps the session open while {@link com.example.student_management.domain.Student#accept}
 * walks the lazy book and course collections. That costs a couple of queries per
 * student, which is fine at this size; a fetch join or batch fetching would be
 * the next step if it grows.
 */
@Service
public class StudentReportServiceImpl implements StudentReportService {
    private final StudentRepository studentRepository;

    public StudentReportServiceImpl(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public <V extends StudentGraphVisitor> V visitAllStudents(V visitor) {
        studentRepository.findAll(Sort.by("name")).forEach(student -> student.accept(visitor));
        return visitor;
    }

    @Override
    @Transactional(readOnly = true)
    public <V extends StudentGraphVisitor> V visitStudent(UUID studentId, V visitor) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId))
                .accept(visitor);
        return visitor;
    }
}
