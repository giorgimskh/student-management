package com.example.student_management.exceptions;

/** The search criteria contradict each other, e.g. a date range that ends before it starts. */
public class InvalidSearchException extends RuntimeException {
    public InvalidSearchException(String message) {
        super(message);
    }
}
