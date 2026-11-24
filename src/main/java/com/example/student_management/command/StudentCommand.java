package com.example.student_management.command;

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
}
