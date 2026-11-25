package com.example.student_management.state;

/** A student in good standing: may do everything, and may move to any other status. */
public final class ActiveState implements StudentState {
    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {
    }

    @Override
    public StudentStatus status() {
        return StudentStatus.ACTIVE;
    }

    @Override
    public boolean canEditProfile() {
        return true;
    }

    @Override
    public boolean canBorrowBooks() {
        return true;
    }

    @Override
    public boolean canEnroll() {
        return true;
    }

    @Override
    public StudentState suspend() {
        return SuspendedState.INSTANCE;
    }

    @Override
    public StudentState graduate() {
        return GraduatedState.INSTANCE;
    }

    @Override
    public StudentState withdraw() {
        return WithdrawnState.INSTANCE;
    }
}
