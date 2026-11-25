package com.example.student_management.state;

/** Something a student must be allowed to do, as decided by their {@link StudentState}. */
public enum StudentPermission {
    /** No status check; always granted. */
    NONE,
    EDIT_PROFILE,
    BORROW_BOOKS,
    ENROLL;

    public boolean isGrantedBy(StudentState state) {
        return switch (this) {
            case NONE -> true;
            case EDIT_PROFILE -> state.canEditProfile();
            case BORROW_BOOKS -> state.canBorrowBooks();
            case ENROLL -> state.canEnroll();
        };
    }
}
