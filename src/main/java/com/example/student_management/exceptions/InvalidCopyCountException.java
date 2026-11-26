package com.example.student_management.exceptions;

/** Asked for fewer than one, or too many, copies of a book at once. */
public class InvalidCopyCountException extends RuntimeException {
    public InvalidCopyCountException(String message) {
        super(message);
    }
}
