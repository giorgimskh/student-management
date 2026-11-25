package com.example.student_management.command;

/**
 * Visitor: one operation over every kind of {@link StudentCommand}. Each concrete
 * command calls back the matching {@code visit} overload from
 * {@link StudentCommand#accept}, so adding a command type means adding a method
 * here, and the compiler then points at every visitor that must handle it.
 *
 * <p>Modern-Java alternative: making {@code StudentCommand} {@code sealed} and
 * using an exhaustive pattern-matching {@code switch} gives the same safety
 * without {@code accept}; this project uses the classic GoF form on purpose.
 *
 * @param <T> what the operation produces
 */
public interface StudentCommandVisitor<T> {
    T visit(AssignBookCommand command);

    T visit(RemoveBookCommand command);

    T visit(EnrollInCourseCommand command);

    T visit(DropCourseCommand command);

    T visit(UpdateStudentCommand command);

    T visit(ChangeStudentStatusCommand command);
}
