package com.example.student_management.command;

import java.util.UUID;

/**
 * Command: one undoable action on a student, packaged as an object so that
 * {@link StudentCommandInvoker} can run it, remember it, and later undo it.
 *
 * @param <R> what {@link #execute()} returns ({@link Void} if nothing)
 */
public interface StudentCommand<R> {
    R execute();

    /** Reverses the effect of {@link #execute()}. */
    void undo();

    /** Human-readable summary, shown in the command history. */
    String description();

    /** The student this command acts on. */
    UUID studentId();

    /**
     * Visitor: double dispatch to the {@code visit} overload for this command's
     * concrete type, so new operations over commands can be added as visitors
     * without changing the command classes.
     */
    <T> T accept(StudentCommandVisitor<T> visitor);
}
