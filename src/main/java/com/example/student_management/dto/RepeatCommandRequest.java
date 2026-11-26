package com.example.student_management.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/** The students to repeat a history entry for. */
public record RepeatCommandRequest(
        @NotEmpty(message = "Choose at least one student")
        @Size(max = RepeatCommandRequest.MAX_STUDENTS, message = "At most " + RepeatCommandRequest.MAX_STUDENTS + " students at once")
        List<@NotNull(message = "Student ids can't be null") UUID> studentIds
) {
    public static final int MAX_STUDENTS = 100;
}
