package com.example.student_management.command;

import com.example.student_management.exceptions.NothingToUndoException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Invoker: runs {@link StudentCommand}s and remembers the successful ones so the
 * most recent can be undone. Only commands that executed without throwing are
 * recorded. History is in memory, shared by all clients, and capped at
 * {@link #MAX_HISTORY} (the oldest entries are dropped).
 */
@Component
public class StudentCommandInvoker {
    static final int MAX_HISTORY = 50;

    private final Deque<StudentCommand<?>> history = new ArrayDeque<>();

    public synchronized <R> R execute(StudentCommand<R> command) {
        R result = command.execute();
        if (history.size() == MAX_HISTORY) {
            history.removeLast();
        }
        history.push(command);
        return result;
    }

    /**
     * Undoes the most recent command and removes it from the history. If the undo
     * fails (e.g. the book was deleted in the meantime) the command is still
     * removed, so one stale entry can't block every later undo.
     */
    public synchronized StudentCommand<?> undoLast() {
        StudentCommand<?> command = history.poll();
        if (command == null) {
            throw new NothingToUndoException("There is no command to undo");
        }
        command.undo();
        return command;
    }

    /** Descriptions of the recorded commands, most recent first. */
    public synchronized List<String> history() {
        return history.stream().map(StudentCommand::description).toList();
    }
}
