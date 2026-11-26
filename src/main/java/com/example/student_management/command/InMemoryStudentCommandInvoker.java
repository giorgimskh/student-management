package com.example.student_management.command;

import com.example.student_management.exceptions.NothingToUndoException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Keeps the command history in memory. Only commands that executed without
 * throwing are recorded. History is shared by all clients and capped at
 * {@link #MAX_HISTORY} (the oldest entries are dropped).
 */
@Component
public class InMemoryStudentCommandInvoker implements StudentCommandInvoker {
    static final int MAX_HISTORY = 50;

    private final Deque<CommandEntry> history = new ArrayDeque<>();
    private long nextId = 1;
    private final UndoGuard undoGuard;

    /** No undo guard: every undo is attempted. */
    public InMemoryStudentCommandInvoker() {
        this(UndoGuard.NONE);
    }

    public InMemoryStudentCommandInvoker(UndoGuard undoGuard) {
        this.undoGuard = undoGuard;
    }

    /** Uses the {@link UndoGuard} bean if there is one. */
    @Autowired
    public InMemoryStudentCommandInvoker(ObjectProvider<UndoGuard> undoGuard) {
        this(undoGuard.getIfAvailable(() -> UndoGuard.NONE));
    }

    @Override
    public synchronized <R> R execute(StudentCommand<R> command) {
        R result = command.execute();
        if (history.size() == MAX_HISTORY) {
            history.removeLast();
        }
        history.push(new CommandEntry(nextId++, command));
        return result;
    }

    /**
     * If the undo fails (e.g. the book was deleted in the meantime) the command is
     * still removed, so one stale entry can't block every later undo. If the
     * {@link UndoGuard} refuses it, the command is kept so it can be retried.
     */
    @Override
    public synchronized StudentCommand<?> undoLast() {
        CommandEntry entry = history.peek();
        if (entry == null) {
            throw new NothingToUndoException("There is no command to undo");
        }
        StudentCommand<?> command = entry.command();
        undoGuard.check(command);
        history.pop();
        command.undo();
        return command;
    }

    @Override
    public synchronized List<String> history() {
        return history.stream().map(entry -> entry.command().description()).toList();
    }

    @Override
    public synchronized List<StudentCommand<?>> commands() {
        return history.stream().<StudentCommand<?>>map(CommandEntry::command).toList();
    }

    @Override
    public synchronized List<CommandEntry> entries() {
        return List.copyOf(history);
    }

    @Override
    public synchronized Optional<StudentCommand<?>> find(long id) {
        return history.stream().filter(entry -> entry.id() == id).<StudentCommand<?>>map(CommandEntry::command).findFirst();
    }
}
