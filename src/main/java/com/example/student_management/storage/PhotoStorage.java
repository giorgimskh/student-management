package com.example.student_management.storage;

import com.example.student_management.domain.Student;
import com.example.student_management.domain.StudentPhoto;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Where student photo bytes live. The service validates uploads and checks the
 * student exists; implementations only persist, load and remove the photo, so
 * the backing store (database, file system, object storage, ...) can change
 * without touching the service.
 */
public interface PhotoStorage {
    /** Stores the photo for the student, replacing any photo they already have. */
    StudentPhoto save(Student student, String originalFilename, String contentType, byte[] data, Instant uploadedAt);

    Optional<StudentPhoto> findByStudentId(UUID studentId);

    /** Removes the student's photo if there is one. */
    void deleteByStudentId(UUID studentId);
}
