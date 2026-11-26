package com.example.student_management.command;

import java.util.List;
import java.util.Optional;

/**
 * Invoker of the command pattern: runs {@link StudentCommand}s and remembers the
 * successful ones so the most recent can be undone.
 */
public interface StudentCommandInvoker {
    /** Executes the command and records it in the history if it did not throw. */
    <R> R execute(StudentCommand<R> command);

    /**
     * Undoes the most recent command and removes it from the history.
     *
     * @throws RuntimeException from the {@link UndoGuard} if the undo is refused; the command then stays in the history
     * @throws com.example.student_management.exceptions.NothingToUndoException if the history is empty
     */
    StudentCommand<?> undoLast();

    /** Descriptions of the recorded commands, most recent first. */
    List<String> history();

    /** The recorded commands themselves, most recent first (e.g. to run a visitor over them). */
    List<StudentCommand<?>> commands();

    /** The recorded commands with their ids, most recent first. */
    List<CommandEntry> entries();

    /** The recorded command with this id, if it is still in the history. */
    Optional<StudentCommand<?>> find(long id);
}
