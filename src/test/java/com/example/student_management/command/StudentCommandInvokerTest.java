package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.exceptions.BookAlreadyAssignedException;
import com.example.student_management.exceptions.NothingToUndoException;
import com.example.student_management.exceptions.ResourceNotFoundException;
import com.example.student_management.service.StudentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentCommandInvokerTest {
    @Mock
    private StudentService studentService;

    private final StudentCommandInvoker invoker = new StudentCommandInvoker();
    private final UUID studentId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();
    private final UUID courseId = UUID.randomUUID();

    @Test
    void execute_returnsCommandResult_andRecordsIt() {
        Student student = Student.builder().id(studentId).build();
        when(studentService.assignBookToStudent(studentId, bookId)).thenReturn(student);

        Student result = invoker.execute(new AssignBookCommand(studentService, studentId, bookId));

        assertThat(result).isSameAs(student);
        assertThat(invoker.history()).containsExactly("Assign book " + bookId + " to student " + studentId);
    }

    @Test
    void undoLast_reversesCommandsInLifoOrder() {
        invoker.execute(new AssignBookCommand(studentService, studentId, bookId));
        invoker.execute(new EnrollInCourseCommand(studentService, studentId, courseId));

        invoker.undoLast();
        invoker.undoLast();

        InOrder order = inOrder(studentService);
        order.verify(studentService).removeStudentFromCourse(studentId, courseId);
        order.verify(studentService).removeBookFromStudent(studentId, bookId);
        assertThat(invoker.history()).isEmpty();
    }

    @Test
    void undoOfRemoveAndDrop_reassignsAndReenrolls() {
        invoker.execute(new RemoveBookCommand(studentService, studentId, bookId));
        invoker.execute(new DropCourseCommand(studentService, studentId, courseId));

        invoker.undoLast();
        invoker.undoLast();

        verify(studentService).enrollInCourse(studentId, courseId);
        verify(studentService).assignBookToStudent(studentId, bookId);
    }

    @Test
    void failedCommand_isNotRecorded() {
        when(studentService.assignBookToStudent(studentId, bookId))
                .thenThrow(new BookAlreadyAssignedException("taken"));

        assertThatThrownBy(() -> invoker.execute(new AssignBookCommand(studentService, studentId, bookId)))
                .isInstanceOf(BookAlreadyAssignedException.class);
        assertThat(invoker.history()).isEmpty();
    }

    @Test
    void undoLast_throws_whenHistoryIsEmpty() {
        assertThatThrownBy(invoker::undoLast).isInstanceOf(NothingToUndoException.class);
    }

    @Test
    void failedUndo_stillDropsTheCommand() {
        invoker.execute(new AssignBookCommand(studentService, studentId, bookId));
        doThrow(new ResourceNotFoundException("gone")).when(studentService).removeBookFromStudent(studentId, bookId);

        assertThatThrownBy(invoker::undoLast).isInstanceOf(ResourceNotFoundException.class);
        assertThat(invoker.history()).isEmpty();
    }

    @Test
    void history_isCapped() {
        for (int i = 0; i < StudentCommandInvoker.MAX_HISTORY + 3; i++) {
            invoker.execute(new DropCourseCommand(studentService, studentId, UUID.randomUUID()));
        }
        assertThat(invoker.history()).hasSize(StudentCommandInvoker.MAX_HISTORY);
    }
}
