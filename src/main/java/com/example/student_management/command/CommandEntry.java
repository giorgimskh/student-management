package com.example.student_management.command;

/**
 * A command in the history with the id the invoker gave it. Ids are never
 * reused, so a client can refer to an entry even after newer commands arrive.
 */
public record CommandEntry(long id, StudentCommand<?> command) {
}
