package com.example.student_management.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidationExceptions(MethodArgumentNotValidException ex){
        Map<String,String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName=error.getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName,errorMessage);
        });

        return ResponseEntity.badRequest().body(errors);
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(String message, HttpStatus status) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<Map<String,String>> handleDuplicateEmail(DuplicateEmailException ex){
        return buildErrorResponse(ex.getMessage(),HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidEnrollmentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidEnrollment(InvalidEnrollmentException ex) {
        return buildErrorResponse(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BookAlreadyAssignedException.class)
    public ResponseEntity<Map<String,String>> handleBookAlreadyAssigned(BookAlreadyAssignedException ex){
        return buildErrorResponse(ex.getMessage(),HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,String>> handleGlobalException(Exception ex){
        Map<String, String> error = new HashMap<>();

        error.put("error", "An unexpected internal server error occurred.");


        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
