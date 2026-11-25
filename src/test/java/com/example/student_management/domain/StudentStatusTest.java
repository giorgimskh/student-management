package com.example.student_management.domain;

import com.example.student_management.exceptions.InvalidStudentStateException;
import com.example.student_management.state.ActiveState;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import com.example.student_management.state.SuspendedState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentStatusTest {

    @Test
    void newStudentsAreActive() {
        assertThat(Student.builder().build().getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(new Student().getStatus()).isEqualTo(StudentStatus.ACTIVE);
    }

    @Test
    void missingStatus_fromRowsCreatedBeforeTheColumn_readsAsActive() {
        Student legacy = Student.builder().status(null).build();

        assertThat(legacy.getStatus()).isEqualTo(StudentStatus.ACTIVE);
        assertThat(legacy.getState()).isSameAs(ActiveState.INSTANCE);
    }

    @Test
    void apply_movesToTheNextStateAndSwapsBehaviour() {
        Student student = Student.builder().build();

        student.apply(StudentTransition.SUSPEND);

        assertThat(student.getStatus()).isEqualTo(StudentStatus.SUSPENDED);
        assertThat(student.getState()).isSameAs(SuspendedState.INSTANCE);
        assertThat(student.getState().canEnroll()).isFalse();
    }

    @Test
    void apply_rejectedTransition_leavesStatusUnchanged() {
        Student student = Student.builder().status(StudentStatus.GRADUATED).build();

        assertThatThrownBy(() -> student.apply(StudentTransition.SUSPEND))
                .isInstanceOf(InvalidStudentStateException.class);
        assertThat(student.getStatus()).isEqualTo(StudentStatus.GRADUATED);
    }
}
