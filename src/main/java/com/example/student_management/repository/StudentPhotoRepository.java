package com.example.student_management.repository;

import com.example.student_management.domain.StudentPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StudentPhotoRepository extends JpaRepository<StudentPhoto, UUID> {
    Optional<StudentPhoto> findByStudentId(UUID studentId);
    void deleteByStudentId(UUID studentId);
}
