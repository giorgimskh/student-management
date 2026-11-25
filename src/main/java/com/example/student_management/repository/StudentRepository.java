package com.example.student_management.repository;

import com.example.student_management.domain.Student;
import com.example.student_management.state.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentRepository extends JpaRepository<Student, UUID> {
    Optional<Student> findByEmail(String email);
    Page<Student> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Student> findByStatus(StudentStatus status, Pageable pageable);
    Page<Student> findByNameContainingIgnoreCaseAndStatus(String name, StudentStatus status, Pageable pageable);
}
