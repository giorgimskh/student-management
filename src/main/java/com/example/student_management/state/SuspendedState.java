package com.example.student_management.state;

/**
 * A suspended student may still update their profile and give back books or drop
 * courses, but may not borrow books or enroll until reactivated.
 */
public final class SuspendedState implements StudentState {
    public static final SuspendedState INSTANCE = new SuspendedState();

    private SuspendedState() {
    }

    @Override
    public StudentStatus status() {
        return StudentStatus.SUSPENDED;
    }

    @Override
    public boolean canEditProfile() {
        return true;
    }

    @Override
    public StudentState reactivate() {
        return ActiveState.INSTANCE;
    }

    @Override
    public StudentState withdraw() {
        return WithdrawnState.INSTANCE;
    }
}
