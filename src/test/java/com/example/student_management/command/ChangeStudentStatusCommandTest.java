package com.example.student_management.command;

import com.example.student_management.domain.Student;
import com.example.student_management.service.StudentService;
import com.example.student_management.state.StudentStatus;
import com.example.student_management.state.StudentTransition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeStudentStatusCommandTest {
    @Mock
    private StudentService studentService;

    private final UUID id = UUID.randomUUID();

    @Test
    void execute_remembersPreviousStatus_andUndoRestoresIt() {
        Student graduated = Student.builder().id(id).status(StudentStatus.GRADUATED).build();
        when(studentService.getStudentById(id)).thenReturn(Student.builder().id(id).status(StudentStatus.ACTIVE).build());
        when(studentService.changeStatus(id, StudentTransition.GRADUATE)).thenReturn(graduated);
        ChangeStudentStatusCommand command = new ChangeStudentStatusCommand(studentService, id, StudentTransition.GRADUATE);

        assertThat(command.execute()).isSameAs(graduated);
        command.undo();

        InOrder order = inOrder(studentService);
        order.verify(studentService).getStudentById(id); // status read before it changes
        order.verify(studentService).changeStatus(id, StudentTransition.GRADUATE);
        order.verify(studentService).restoreStatus(id, StudentStatus.ACTIVE);
    }

    @Test
    void undoThroughTheInvoker_putsASuspendedStudentBack() {
        when(studentService.getStudentById(id)).thenReturn(Student.builder().id(id).status(StudentStatus.SUSPENDED).build());
        StudentCommandInvoker invoker = new InMemoryStudentCommandInvoker();

        invoker.execute(new ChangeStudentStatusCommand(studentService, id, StudentTransition.WITHDRAW));
        StudentCommand<?> undone = invoker.undoLast();

        assertThat(undone.description()).isEqualTo("Withdraw student " + id);
        assertThat(invoker.history()).isEmpty();
        verify(studentService).restoreStatus(id, StudentStatus.SUSPENDED);
    }

    @Test
    void description_namesTheTransition() {
        assertThat(new ChangeStudentStatusCommand(studentService, id, StudentTransition.REACTIVATE).description())
                .isEqualTo("Reactivate student " + id);
    }
}
