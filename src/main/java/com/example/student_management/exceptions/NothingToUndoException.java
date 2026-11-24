package com.example.student_management.exceptions;

public class NothingToUndoException extends RuntimeException {
    public NothingToUndoException(String message) {
        super(message);
    }
}
