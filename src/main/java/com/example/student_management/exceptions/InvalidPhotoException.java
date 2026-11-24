package com.example.student_management.exceptions;

public class InvalidPhotoException extends RuntimeException {
    public InvalidPhotoException(String message) {
        super(message);
    }
}
