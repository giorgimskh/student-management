package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.CommandNotRepeatableException;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** {@link StudentCommand#copyFor(UUID)}: each command as a prototype for the same action on another student. */
@ExtendWith(MockitoExtension.class)
class CommandPrototypeTest {
    @Mock
    private StudentService studentService;

    private final UUID ana = UUID.randomUUID();
    private final UUID beka = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();

    @Test
    void enrollCopy_enrollsTheOtherStudentInTheSameCourse() {
        EnrollInCourseCommand copy = new EnrollInCourseCommand(studentService, ana, courseId).copyFor(beka);

        assertThat(copy.studentId()).isEqualTo(beka);
        assertThat(copy.courseId()).isEqualTo(courseId);
        copy.execute();
        verify(studentService).enrollInCourse(beka, courseId);
    }

    @Test
    void dropCopy_dropsTheOtherStudentFromTheSameCourse() {
        DropCourseCommand copy = new DropCourseCommand(studentService, ana, courseId).copyFor(beka);

        copy.execute();
        verify(studentService).removeStudentFromCourse(beka, courseId);
    }

    @Test
    void statusCopy_keepsTheTransition_butRemembersItsOwnPreviousStatus() {
        when(studentService.getStudentById(ana)).thenReturn(Student.builder().id(ana).status(StudentStatus.ACTIVE).build());
        when(studentService.getStudentById(beka)).thenReturn(Student.builder().id(beka).status(StudentStatus.SUSPENDED).build());
        ChangeStudentStatusCommand prototype = new ChangeStudentStatusCommand(studentService, ana, StudentTransition.WITHDRAW);
        prototype.execute();

        ChangeStudentStatusCommand copy = prototype.copyFor(beka);
        copy.execute();
        copy.undo();
        prototype.undo();

        assertThat(copy).isNotSameAs(prototype);
        assertThat(copy.transition()).isEqualTo(StudentTransition.WITHDRAW);
        verify(studentService).changeStatus(beka, StudentTransition.WITHDRAW);
        verify(studentService).restoreStatus(beka, StudentStatus.SUSPENDED); // the copy's own "before", not Ana's
        verify(studentService).restoreStatus(ana, StudentStatus.ACTIVE);
    }

    @Test
    void repeatableCommands() {
        assertThat(new EnrollInCourseCommand(studentService, ana, courseId).repeatable()).isTrue();
        assertThat(new DropCourseCommand(studentService, ana, courseId).repeatable()).isTrue();
        assertThat(new ChangeStudentStatusCommand(studentService, ana, StudentTransition.SUSPEND).repeatable()).isTrue();
    }

    @Test
    void bookAndProfileCommands_cannotBeCopied() {
        List<StudentCommand<?>> notRepeatable = List.of(
                new AssignBookCommand(studentService, ana, bookId),
                new RemoveBookCommand(studentService, ana, bookId),
                new UpdateStudentCommand(studentService, ana, Student.builder().name("Ana").build()));

        for (StudentCommand<?> command : notRepeatable) {
            assertThat(command.repeatable()).as(command.getClass().getSimpleName()).isFalse();
            assertThatThrownBy(() -> command.copyFor(beka))
                    .as(command.getClass().getSimpleName())
                    .isInstanceOf(CommandNotRepeatableException.class);
        }
    }
}
