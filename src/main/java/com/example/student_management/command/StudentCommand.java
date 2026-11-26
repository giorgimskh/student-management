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
     * Prototype: a new command that does what this one does, but to another
     * student, e.g. "enroll Ana in CS101" copied for Beka. The copy has not been
     * executed and keeps none of this command's undo state, so it can be run and
     * undone on its own. Callers need not know the concrete command class.
     *
     * @throws com.example.student_management.exceptions.CommandNotRepeatableException if {@link #repeatable()} is false
     */
    StudentCommand<R> copyFor(UUID studentId);

    /** Whether {@link #copyFor(UUID)} is supported. */
    default boolean repeatable() {
        return true;
    }

    /**
     * Visitor: double dispatch to the {@code visit} overload for this command's
     * concrete type, so new operations over commands can be added as visitors
     * without changing the command classes.
     */
    <T> T accept(StudentCommandVisitor<T> visitor);
}
