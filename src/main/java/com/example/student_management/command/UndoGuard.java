package com.example.student_management.command;

/**
 * Checked by the invoker before an undo. If it throws, the command stays in the
 * history, so the undo can be retried once the reason is gone.
 */
@FunctionalInterface
public interface UndoGuard {
    UndoGuard NONE = command -> { };

    void check(StudentCommand<?> command);
}
