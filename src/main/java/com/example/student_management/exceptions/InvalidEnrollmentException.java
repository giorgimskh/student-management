package com.example.student_management.exceptions;

public class InvalidEnrollmentException extends RuntimeException{
    public InvalidEnrollmentException(String message) {
        super(message);
    }
}
