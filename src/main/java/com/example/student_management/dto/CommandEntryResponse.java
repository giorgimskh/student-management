package com.example.student_management.dto;

import java.util.UUID;

/** One undo history entry: who it acted on, and whether it can be repeated for other students. */
public record CommandEntryResponse(long id, String description, UUID studentId, boolean repeatable) {
}
