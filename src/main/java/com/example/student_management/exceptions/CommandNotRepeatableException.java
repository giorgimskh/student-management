package com.example.student_management.exceptions;

/** The command cannot be copied to another student, e.g. lending a book that only one student can hold. */
public class CommandNotRepeatableException extends RuntimeException {
    public CommandNotRepeatableException(String message) {
        super(message);
    }
}
