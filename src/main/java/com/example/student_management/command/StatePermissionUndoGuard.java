package com.example.student_management.command;

import com.example.student_management.exceptions.InvalidStudentStateException;
import com.example.student_management.repository.StudentRepository;
import com.example.student_management.state.StudentPermission;
import org.springframework.stereotype.Component;

/**
 * Refuses an undo (409) when the student's current state does not grant the
 * permission the undo needs, as worked out by {@link RequiredPermissionVisitor#FOR_UNDO}.
 * Example: a book was removed, then the student was suspended; undoing the
 * removal would assign a book to a suspended student.
 *
 * <p>Unlike a failed undo, a refused one keeps the command in the history: after
 * the student is reactivated the same undo works. A deleted student is let
 * through so the undo fails and the stale entry is dropped.
 */
@Component
public class StatePermissionUndoGuard implements UndoGuard {
    private final StudentRepository studentRepository;
    private final CommandDescriber describer;

    public StatePermissionUndoGuard(StudentRepository studentRepository, CommandDescriber describer) {
        this.studentRepository = studentRepository;
        this.describer = describer;
    }

    @Override
    public void check(StudentCommand<?> command) {
        StudentPermission needed = command.accept(RequiredPermissionVisitor.FOR_UNDO);
        if (needed == StudentPermission.NONE) {
            return;
        }
        studentRepository.findById(command.studentId())
                .filter(student -> !needed.isGrantedBy(student.getState()))
                .ifPresent(student -> {
                    throw new InvalidStudentStateException("Cannot undo \"" + describer.describe(command)
                            + "\": student is " + student.getStatus());
                });
    }
}
