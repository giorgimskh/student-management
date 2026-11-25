package com.example.student_management.exceptions;

/** The student's current status does not allow the requested action or transition (HTTP 409). */
public class InvalidStudentStateException extends RuntimeException {
    public InvalidStudentStateException(String message) {
        super(message);
    }
}
