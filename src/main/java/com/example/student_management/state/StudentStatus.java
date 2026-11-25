package com.example.student_management.state;

/**
 * The persisted form of a student's state (stored as a string column). The
 * behaviour lives in the {@link StudentState} object returned by {@link #state()}.
 */
public enum StudentStatus {
    ACTIVE,
    SUSPENDED,
    GRADUATED,
    WITHDRAWN;

    public StudentState state() {
        return switch (this) {
            case ACTIVE -> ActiveState.INSTANCE;
            case SUSPENDED -> SuspendedState.INSTANCE;
            case GRADUATED -> GraduatedState.INSTANCE;
            case WITHDRAWN -> WithdrawnState.INSTANCE;
        };
    }
}
