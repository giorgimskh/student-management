package com.example.student_management.exceptions;

public class BookAlreadyAssignedException extends RuntimeException{
    public BookAlreadyAssignedException(String message) {
        super(message);
    }
}
