package com.example.student_management.command;

import com.example.student_management.exceptions.NothingToUndoException;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Keeps the command history in memory. Only commands that executed without
 * throwing are recorded. History is shared by all clients and capped at
 * {@link #MAX_HISTORY} (the oldest entries are dropped).
 */
@Component
public class InMemoryStudentCommandInvoker implements StudentCommandInvoker {
    static final int MAX_HISTORY = 50;

    private final Deque<StudentCommand<?>> history = new ArrayDeque<>();

    @Override
    public synchronized <R> R execute(StudentCommand<R> command) {
        R result = command.execute();
        if (history.size() == MAX_HISTORY) {
            history.removeLast();
        }
        history.push(command);
        return result;
    }

    /**
     * If the undo fails (e.g. the book was deleted in the meantime) the command is
     * still removed, so one stale entry can't block every later undo.
     */
    @Override
    public synchronized StudentCommand<?> undoLast() {
        StudentCommand<?> command = history.poll();
        if (command == null) {
            throw new NothingToUndoException("There is no command to undo");
        }
        command.undo();
        return command;
    }

    @Override
    public synchronized List<String> history() {
        return history.stream().map(StudentCommand::description).toList();
    }
}
