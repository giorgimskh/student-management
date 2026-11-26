package com.example.student_management.command;

import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

/**
 * Repeats a command from the history for other students, using the command as
 * a prototype: each student gets a {@link StudentCommand#copyFor(UUID) copy},
 * run through the invoker like any other command, so every copy appears in the
 * history and can be undone on its own.
 *
 * <p>Students are handled one by one. One student failing (e.g. already
 * enrolled, or suspended) does not stop the others; each gets its own result.
 */
@Component
public class CommandRepeater {
    private static final Logger log = LoggerFactory.getLogger(CommandRepeater.class);

    private final StudentCommandInvoker invoker;
    private final CommandDescriber describer;

    public CommandRepeater(StudentCommandInvoker invoker, CommandDescriber describer) {
        this.invoker = invoker;
        this.describer = describer;
    }

    public enum Outcome { DONE, FAILED, SKIPPED }

    /** What happened for one student. {@code message} is the action done, or why it was not. */
    public record Result(UUID studentId, Outcome outcome, String message) {
    }

    /**
     * @param entryId    the history entry to use as the prototype
     * @param studentIds who to repeat it for; duplicates are ignored
     * @throws ResourceNotFoundException     if the entry is no longer in the history
     * @throws CommandNotRepeatableException if that kind of command cannot be copied
     */
    public List<Result> repeat(long entryId, Collection<UUID> studentIds) {
        StudentCommand<?> prototype = invoker.find(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("No command #" + entryId + " in the history"));
        if (!prototype.repeatable()) {
            prototype.copyFor(prototype.studentId()); // throws, giving this kind of command's own reason
            throw new CommandNotRepeatableException("This action can't be repeated for another student");
        }
        List<Result> results = new ArrayList<>();
        for (UUID studentId : new LinkedHashSet<>(studentIds)) {
            results.add(repeatFor(prototype, studentId));
        }
        return results;
    }

    private Result repeatFor(StudentCommand<?> prototype, UUID studentId) {
        if (studentId.equals(prototype.studentId())) {
            return new Result(studentId, Outcome.SKIPPED, "This is the student the action was done to");
        }
        StudentCommand<?> copy = prototype.copyFor(studentId);
        try {
            invoker.execute(copy);
        } catch (RuntimeException e) {
            return new Result(studentId, Outcome.FAILED, reason(e));
        }
        return new Result(studentId, Outcome.DONE, describer.describe(copy));
    }

    /** The application's own exceptions explain themselves; anything else is logged, not shown. */
    private static String reason(RuntimeException e) {
        if (e.getClass().getPackageName().equals(ResourceNotFoundException.class.getPackageName())) {
            return e.getMessage();
        }
        log.error("Repeating a command failed unexpectedly", e);
        return "Unexpected error";
    }
}
