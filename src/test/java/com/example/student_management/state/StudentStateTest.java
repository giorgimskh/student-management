package com.example.student_management.state;

import com.example.student_management.exceptions.InvalidStudentStateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentStateTest {

    @ParameterizedTest(name = "{0}: edit={1} borrow={2} enroll={3}")
    @CsvSource({
            "ACTIVE,    true,  true,  true",
            "SUSPENDED, true,  false, false",
            "GRADUATED, false, false, false",
            "WITHDRAWN, false, false, false"
    })
    void permissionsPerState(StudentStatus status, boolean edit, boolean borrow, boolean enroll) {
        StudentState state = status.state();

        assertThat(state.canEditProfile()).isEqualTo(edit);
        assertThat(state.canBorrowBooks()).isEqualTo(borrow);
        assertThat(state.canEnroll()).isEqualTo(enroll);
    }

    /** Every (state, transition) pair: the expected target, or "-" when it must be rejected. */
    @ParameterizedTest(name = "{0} --{1}--> {2}")
    @CsvSource({
            "ACTIVE,    SUSPEND,    SUSPENDED",
            "ACTIVE,    REACTIVATE, -",
            "ACTIVE,    GRADUATE,   GRADUATED",
            "ACTIVE,    WITHDRAW,   WITHDRAWN",
            "SUSPENDED, SUSPEND,    -",
            "SUSPENDED, REACTIVATE, ACTIVE",
            "SUSPENDED, GRADUATE,   -",
            "SUSPENDED, WITHDRAW,   WITHDRAWN",
            "GRADUATED, SUSPEND,    -",
            "GRADUATED, REACTIVATE, -",
            "GRADUATED, GRADUATE,   -",
            "GRADUATED, WITHDRAW,   -",
            "WITHDRAWN, SUSPEND,    -",
            "WITHDRAWN, REACTIVATE, -",
            "WITHDRAWN, GRADUATE,   -",
            "WITHDRAWN, WITHDRAW,   -"
    })
    void transitions(StudentStatus from, StudentTransition transition, String expected) {
        if (expected.equals("-")) {
            assertThatThrownBy(() -> transition.applyTo(from.state()))
                    .isInstanceOf(InvalidStudentStateException.class)
                    .hasMessage("Cannot " + transition.name().toLowerCase() + " a student who is " + from);
        } else {
            assertThat(transition.applyTo(from.state()).status()).isEqualTo(StudentStatus.valueOf(expected));
        }
    }

    @ParameterizedTest
    @EnumSource(StudentStatus.class)
    void eachStatusMapsToAStateThatReportsIt(StudentStatus status) {
        assertThat(status.state().status()).isEqualTo(status);
        assertThat(status.state()).isSameAs(status.state()); // stateless singletons
    }
}
