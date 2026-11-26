package com.example.student_management.repository;

import com.example.student_management.domain.Student;
import com.example.student_management.state.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Students. Filtered listing goes through {@link JpaSpecificationExecutor} with a
 * {@link com.example.student_management.search.StudentSearch}; the derived
 * finders below are kept for existing callers.
 */
public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {
    Optional<Student> findByEmail(String email);
    Page<Student> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<Student> findByStatus(StudentStatus status, Pageable pageable);
    Page<Student> findByNameContainingIgnoreCaseAndStatus(String name, StudentStatus status, Pageable pageable);
}
