package com.example.student_management.command;

import com.example.student_management.state.StudentPermission;

/**
 * Visitor that tells which {@link StudentPermission} the student's current state
 * must grant for a command to run ({@link #FOR_EXECUTE}) or to be undone
 * ({@link #FOR_UNDO}). Undo is usually the opposite action, so it can need a
 * different permission: undoing "remove book" assigns the book again and so
 * needs {@link StudentPermission#BORROW_BOOKS}.
 *
 * <p>Status changes need {@link StudentPermission#NONE}: the transition itself is
 * checked by the state, and undo restores the old status directly.
 */
public final class RequiredPermissionVisitor implements StudentCommandVisitor<StudentPermission> {
    public static final RequiredPermissionVisitor FOR_EXECUTE = new RequiredPermissionVisitor(false);
    public static final RequiredPermissionVisitor FOR_UNDO = new RequiredPermissionVisitor(true);

    private final boolean undo;

    private RequiredPermissionVisitor(boolean undo) {
        this.undo = undo;
    }

    @Override
    public StudentPermission visit(AssignBookCommand command) {
        return undo ? StudentPermission.NONE : StudentPermission.BORROW_BOOKS;
    }

    @Override
    public StudentPermission visit(RemoveBookCommand command) {
        return undo ? StudentPermission.BORROW_BOOKS : StudentPermission.NONE;
    }

    @Override
    public StudentPermission visit(EnrollInCourseCommand command) {
        return undo ? StudentPermission.NONE : StudentPermission.ENROLL;
    }

    @Override
    public StudentPermission visit(DropCourseCommand command) {
        return undo ? StudentPermission.ENROLL : StudentPermission.NONE;
    }

    @Override
    public StudentPermission visit(UpdateStudentCommand command) {
        return StudentPermission.EDIT_PROFILE;
    }

    @Override
    public StudentPermission visit(ChangeStudentStatusCommand command) {
        return StudentPermission.NONE;
    }
}
