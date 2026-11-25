package com.example.student_management.service;

import com.example.student_management.visitor.StudentGraphVisitor;

import java.util.UUID;

/** Runs a {@link StudentGraphVisitor} over the student object graph. */
public interface StudentReportService {
    /** Every student, with their books, courses and photo, visited in one read-only transaction. */
    <V extends StudentGraphVisitor> V visitAllStudents(V visitor);

    /**
     * One student, with their books, courses and photo.
     *
     * @throws com.example.student_management.exceptions.ResourceNotFoundException if there is no such student
     */
    <V extends StudentGraphVisitor> V visitStudent(UUID studentId, V visitor);
}
