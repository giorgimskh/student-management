package com.example.student_management.state;

import java.util.function.UnaryOperator;

/** The status changes a client can ask for; each one delegates to the current {@link StudentState}. */
public enum StudentTransition {
    SUSPEND(StudentState::suspend),
    REACTIVATE(StudentState::reactivate),
    GRADUATE(StudentState::graduate),
    WITHDRAW(StudentState::withdraw);

    private final UnaryOperator<StudentState> step;

    StudentTransition(UnaryOperator<StudentState> step) {
        this.step = step;
    }

    /** Returns the state this transition leads to, or throws if {@code from} does not allow it. */
    public StudentState applyTo(StudentState from) {
        return step.apply(from);
    }
}
